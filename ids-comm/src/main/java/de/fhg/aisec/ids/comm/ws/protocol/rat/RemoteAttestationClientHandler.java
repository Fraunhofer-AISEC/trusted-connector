/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
import de.fhg.aisec.ids.comm.IdscpConfiguration;
import de.fhg.aisec.ids.comm.ws.protocol.fsm.Event;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.RemoteToTpm2d;
import de.fhg.aisec.ids.messages.AttestationProtos.RemoteToTpm2d.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.Tpm2dToRemote;
import de.fhg.aisec.ids.messages.Idscp.*;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAttestationClientHandler extends RemoteAttestationHandler {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteAttestationClientHandler.class);
  private byte[] myNonce;
  private byte[] yourNonce;
  private IdsAttestationType aType;
  private long sessionID = 0; // used to count messages between ids connectors during attestation
  private final URI ttpUri;
  private final int attestationMask;
  private final CertificatePair certificatePair;

  public RemoteAttestationClientHandler(
      @NonNull IdscpConfiguration clientConfiguration,
      @Nullable URI ttpUri,
      @Nullable String socket) {
    // set ttp uri
    this.ttpUri = ttpUri;
    // set current attestation type and mask (see attestation.proto)
    this.aType = clientConfiguration.getAttestationType();
    this.attestationMask = clientConfiguration.getAttestationMask();
    this.certificatePair = clientConfiguration.getCertificatePair();
  }

  public MessageLite enterRatRequest(@NonNull Event e) {
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

  public MessageLite sendTPM2Ddata(@NonNull Event e) {
    // get nonce from server msg
    this.yourNonce = e.getMessage().getAttestationRequest().getNonce().toByteArray();
    final byte[] hash = calculateHash(this.yourNonce, certificatePair.getRemoteCertificate());

    if (++this.sessionID != e.getMessage().getId()) {
      return RemoteAttestationHandler.sendError(
          ++this.sessionID, "Invalid session ID " + e.getMessage().getId());
    }

    String halg = "";
    ByteString quoted = ByteString.EMPTY;
    ByteString signature = ByteString.EMPTY;
    List<Pcr> pcrValues = Collections.emptyList();
    ByteString certificate = ByteString.EMPTY;
    if (tpm2dSocket != null) {
      try {
        RemoteToTpm2d.Builder msgBuilder =
            RemoteToTpm2d.newBuilder()
                .setAtype(this.aType)
                .setQualifyingData(ByteString.copyFrom(hash))
                .setCode(Code.ATTESTATION_REQ)
                .setPcrs(this.attestationMask);
        if (this.aType.equals(IdsAttestationType.ADVANCED)) {
          // send msg to local unix socket with bitmask set
          // construct protobuf message to send to local tpm2d via unix socket
          msgBuilder.setPcrs(this.attestationMask);
        }
        Tpm2dToRemote response = tpm2dSocket.requestAttestation(msgBuilder.build());
        halg = response.getHalg().name();
        quoted = response.getQuoted();
        signature = response.getSignature();
        pcrValues = response.getPcrValuesList();
        certificate = response.getCertificate();
      } catch (IOException ex) {
        lastError = "IOException during communication with tpm2d: " + ex.getMessage();
        LOG.error(lastError, ex);
      }
    } else {
      LOG.warn("Tpm2dSocket is not available. No TPM present?");
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
                .setCertificate(certificate)
                .build())
        .build();
  }

  public MessageLite sendResult(@NonNull Event e) {
    final byte[] hash = calculateHash(this.myNonce, certificatePair.getLocalCertificate());
    AttestationResponse response = e.getMessage().getAttestationResponse();
    assert response != null;

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
          ++this.sessionID, RemoteAttestationHandler.lastError);
    }

    if (this.checkSignature(response, hash) && checkRepository(this.aType, response, ttpUri)) {
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

  public MessageLite leaveRatRequest(@NonNull Event e) {
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
          ++this.sessionID, RemoteAttestationHandler.lastError);
    }

    return ConnectorMessage.newBuilder()
        .setId(++this.sessionID)
        .setType(ConnectorMessage.Type.RAT_LEAVE)
        .setAttestationLeave(AttestationLeave.newBuilder().setAtype(this.aType).build())
        .build();
  }
}
