/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.comm.CertificatePair;
import de.fhg.aisec.ids.comm.Converter;
import de.fhg.aisec.ids.comm.IdscpConfiguration;
import de.fhg.aisec.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.aisec.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Event;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/** Implements the handling of individual protocol steps in the IDS remote attestation protocol. */
public class RemoteAttestationServerHandler extends RemoteAttestationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteAttestationServerHandler.class);
  private byte[] myNonce;
  private byte[] yourNonce;
  private Thread thread;
  private UnixSocketThread client;
  private UnixSocketResponseHandler handler;
  private long sessionID = 0; // used to count messages between ids connectors during attestation
  private final URI ttpUri;
  private final int attestationMask;
  private final IdsAttestationType aType;
  private final CertificatePair certificatePair;
  private AttestationResponse resp;

  public RemoteAttestationServerHandler(
      IdscpConfiguration serverConfiguration,
      URI ttpUri,
      String socket) {
    // set ttp uri
    this.ttpUri = ttpUri;
    // set current attestation type and mask (see attestation.proto)
    this.aType = serverConfiguration.getAttestationType();
    this.attestationMask = serverConfiguration.getAttestationMask();
    this.certificatePair = serverConfiguration.getCertificatePair();
    // try to start new Thread:
    // UnixSocketThread will be used to communicate with local TPM2d
    try {
      // client will be used to send messages
      this.client = new UnixSocketThread(socket);
      this.thread = new Thread(client);
      this.thread.setDaemon(true);
      this.thread.start();
      // responseHandler will be used to wait for messages
      this.handler = new UnixSocketResponseHandler();
    } catch (IOException e) {
      LOG.warn("could not write to/read from {}", socket);
    }
  }

  public MessageLite enterRatRequest(Event e) {
    this.yourNonce = e.getMessage().getAttestationRequest().getNonce().toByteArray();
    // generate a new software nonce on the client and send it to server
    this.myNonce = NonceGenerator.generate(20);
    // get starting session id
    this.sessionID = e.getMessage().getId();
    return ConnectorMessage.newBuilder()
        .setId(++this.sessionID)
        .setType(ConnectorMessage.Type.RAT_REQUEST)
        .setAttestationRequest(
            AttestationRequest.newBuilder()
                .setAtype(this.aType)
                .setNonce(ByteString.copyFrom(this.myNonce))
                .build())
        .build();
  }

  public MessageLite sendTPM2Ddata(Event e) {
    // temporarily save attestation response in order to check it in the result phase
    this.resp = e.getMessage().getAttestationResponse();
    final byte[] hash = calculateHash(this.yourNonce, certificatePair.getRemoteCertificate());

    if (++this.sessionID != e.getMessage().getId()) {
      return sendError(
          this.thread,
          ++this.sessionID,
          "error: sessionID not correct ! (is "
              + e.getMessage().getId()
              + " but should have been "
              + (this.sessionID + 1)
              + ")");
    }

    String halg = "";
    ByteString quoted = ByteString.EMPTY;
    ByteString signature = ByteString.EMPTY;
    List<Pcr> pcrValues = Collections.emptyList();
    ByteString aikCertificate = ByteString.EMPTY;
    if (thread != null && thread.isAlive()) {
      try {
        ControllerToTpm.Builder msgBuilder = ControllerToTpm.newBuilder()
            .setAtype(this.aType)
            .setQualifyingData(Converter.bytesToHex(hash))
            .setCode(Code.INTERNAL_ATTESTATION_REQ)
            .setPcrs(this.attestationMask);
        if (this.aType.equals(IdsAttestationType.ADVANCED)) {
          // send msg to local unix socket with bitmask set
          // construct protobuf message to send to local tpm2d via unix socket
          msgBuilder.setPcrs(this.attestationMask);
        }
        ControllerToTpm msg = msgBuilder.build();
        LOG.debug("ControllerToTpm message: {}", msg.toString());
        client.send(msg.toByteArray(), this.handler, true);
        // and wait for response
        byte[] toParse = this.handler.waitForResponse();
        TpmToController response = TpmToController.parseFrom(toParse);
        LOG.debug("TpmToController message: {}", response.toString());
        halg = response.getHalg();
        quoted = Converter.hexToByteString(response.getQuoted());
        signature = Converter.hexToByteString(response.getSignature());
        pcrValues = response.getPcrValuesList();
        aikCertificate = Converter.hexToByteString(response.getCertificateUri());
      } catch (IOException ex) {
        lastError = "error: IOException when talking to tpm2d :" + ex.getMessage();
        client.terminate();
      } catch (InterruptedException ex) {
        lastError = "error: InterruptedException when talking to tpm2d :" + ex.getMessage();
        client.terminate();
        Thread.currentThread().interrupt();
      }
    } else {
      lastError = "error: RAT client thread is not alive !";
    }
    // now return values from answer to server
    return ConnectorMessage.newBuilder()
        .setId(++this.sessionID)
        .setType(ConnectorMessage.Type.RAT_RESPONSE)
        .setAttestationResponse(
            AttestationResponse.newBuilder()
                .setAtype(this.aType)
                .setHalg(halg)
                .setQuoted(quoted)
                .setSignature(signature)
                .addAllPcrValues(pcrValues)
                .setAikCertificate(aikCertificate)
                .build())
        .build();
  }

  public MessageLite sendResult(Event e) {
    final byte[] hash = calculateHash(this.myNonce, certificatePair.getLocalCertificate());

    if (++this.sessionID == e.getMessage().getId()) {
      if (this.checkSignature(this.resp, hash) &&
          checkRepository(this.aType, this.resp, ttpUri)) {
        this.mySuccess = true;
      } else {
        lastError = "error: signature check not ok";
      }
      return ConnectorMessage.newBuilder()
          .setId(++this.sessionID)
          .setType(ConnectorMessage.Type.RAT_RESULT)
          .setAttestationResult(
              AttestationResult.newBuilder().setAtype(this.aType).setResult(this.mySuccess).build())
          .build();
    } else {
      lastError =
          "error: sessionID not correct ! (is "
              + e.getMessage().getId()
              + " but should have been "
              + (this.sessionID + 1)
              + ")";
    }
    LOG.debug(lastError);
    return sendError(
        this.thread, ++this.sessionID, lastError);
  }

  public MessageLite leaveRatRequest(Event e) {
    this.yourSuccess = e.getMessage().getAttestationResult().getResult();
    if (this.thread != null) {
      this.thread.interrupt();
    }
    if (++this.sessionID == e.getMessage().getId()) {
      return ConnectorMessage.newBuilder()
          .setId(++this.sessionID)
          .setType(ConnectorMessage.Type.RAT_LEAVE)
          .setAttestationLeave(AttestationLeave.newBuilder().setAtype(this.aType).build())
          .build();
    }
    lastError =
        "error: sessionID not correct ! (is "
            + e.getMessage().getId()
            + " but should have been "
            + (this.sessionID + 1)
            + ")";
    LOG.debug(lastError);
    return sendError(
        this.thread, ++this.sessionID, lastError);
  }
}
