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
package de.fhg.ids.comm.ws.protocol.rat;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.CertificatePair;
import de.fhg.ids.comm.Converter;
import de.fhg.ids.comm.IdscpConfiguration;
import de.fhg.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAttestationClientHandler extends RemoteAttestationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteAttestationClientHandler.class);
  private byte[] myNonce;
  private byte[] yourNonce;
  private IdsAttestationType aType;
  private UnixSocketResponseHandler handler;
  private UnixSocketThread tpmClient;
  private Thread thread;
  private long sessionID = 0; // used to count messages between ids connectors during attestation
  private final URI ttpUri;
  private final int attestationMask;
  private final CertificatePair certificatePair;

  public RemoteAttestationClientHandler(
      IdscpConfiguration clientConfiguration,
      URI ttpUri,
      String socket) {
    // set ttp uri
    this.ttpUri = ttpUri;
    // set current attestation type and mask (see attestation.proto)
    this.aType = clientConfiguration.getAttestationType();
    this.attestationMask = clientConfiguration.getAttestationMask();
    this.certificatePair = clientConfiguration.getCertificatePair();
    // try to start new Thread:
    // UnixSocketThread will be used to communicate with local TPM2d
    try {
      // client will be used to send messages
      this.tpmClient = new UnixSocketThread(socket);
      this.thread = new Thread(tpmClient);
      this.thread.setDaemon(true);
      this.thread.start();
      // responseHandler will be used to wait for messages
      this.handler = new UnixSocketResponseHandler();
    } catch (IOException e) {
      lastError = "could not write to/read from " + socket;
      LOG.warn(lastError);
    }
  }

  public MessageLite enterRatRequest(Event e) {
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
    // get nonce from server msg
    this.yourNonce = e.getMessage().getAttestationRequest().getNonce().toByteArray();
    final byte[] hash = calculateHash(this.yourNonce, certificatePair.getRemoteCertificate());

    if (++this.sessionID != e.getMessage().getId()) {
      return RemoteAttestationHandler.sendError(
          this.thread, ++this.sessionID, "Invalid session ID " + e.getMessage().getId());
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
        LOG.debug(msg.toString());
        tpmClient.send(msg.toByteArray(), this.handler, true);
        // and wait for response
        byte[] toParse = this.handler.waitForResponse();
        TpmToController response = TpmToController.parseFrom(toParse);
        halg = response.getHalg();
        quoted = Converter.hexToByteString(response.getQuoted());
        signature = Converter.hexToByteString(response.getSignature());
        pcrValues = response.getPcrValuesList();
        aikCertificate = Converter.hexToByteString(response.getCertificateUri());
      } catch (IOException ex) {
        lastError = "error: IOException when talking to tpm2d :" + ex.getMessage();
        tpmClient.terminate();
      } catch (InterruptedException ex) {
        lastError = "error: InterruptedException when talking to tpm2d :" + ex.getMessage();
        tpmClient.terminate();
        Thread.currentThread().interrupt();
      }
    } else {
      LOG.warn("error: RAT client thread is not alive. No TPM present?");
    }
    // now return values from answer to provider
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
    AttestationResponse response = e.getMessage().getAttestationResponse();

    // Abort on wrong session ID
    if (++this.sessionID != e.getMessage().getId()) {
      lastError =
          "error: sessionID not correct ! (is "
              + e.getMessage().getId()
              + " but should have been "
              + (this.sessionID + 1)
              + ")";
      LOG.debug(lastError);
      return RemoteAttestationHandler.sendError(
          this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
    }

    if (this.checkSignature(response, hash)
        && RemoteAttestationHandler.checkRepository(this.aType, response, ttpUri)) {
      this.mySuccess = true;
    } else {
      LOG.warn(
          "Could not verify signature or could not validate PCR values via trusted third party. "
              + "Remote attestation failed.");
    }

    return ConnectorMessage.newBuilder()
        .setId(++this.sessionID)
        .setType(ConnectorMessage.Type.RAT_RESULT)
        .setAttestationResult(
            AttestationResult.newBuilder().setAtype(this.aType).setResult(this.mySuccess).build())
        .build();
  }

  public MessageLite leaveRatRequest(Event e) {
    if (this.thread != null) {
      this.thread.interrupt();
    }

    // Abort on wrong session ID
    if (++this.sessionID != e.getMessage().getId()) {
      lastError =
          "error: sessionID not correct ! (is "
              + e.getMessage().getId()
              + " but should have been "
              + (this.sessionID + 1)
              + ")";
      LOG.debug(lastError);
      return RemoteAttestationHandler.sendError(
          this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
    }

    return ConnectorMessage.newBuilder()
        .setId(++this.sessionID)
        .setType(ConnectorMessage.Type.RAT_LEAVE)
        .setAttestationLeave(AttestationLeave.newBuilder().setAtype(this.aType).build())
        .build();
  }
}
