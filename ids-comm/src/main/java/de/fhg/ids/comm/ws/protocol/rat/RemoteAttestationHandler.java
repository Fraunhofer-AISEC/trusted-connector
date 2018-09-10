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

import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.api.conm.RatResult;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.ids.comm.CertificatePair;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAttestationHandler {
  protected static final Logger LOG =
      LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
  protected static String lastError = "";
  // used to count messages between ids connector and attestation repository
  protected static long privateID = new java.util.Random().nextLong();
  protected boolean mySuccess = false;
  protected boolean yourSuccess = false;

  public RatResult getAttestationResult() {
    LOG.debug("your success: {}    my success: {}", this.yourSuccess, this.mySuccess);
    if (!this.mySuccess) {
      return new RatResult(RatResult.Status.FAILED, "Could not verify");
    }

    if (!this.yourSuccess) {
      return new RatResult(RatResult.Status.FAILED, "Remote party did not verify successfully");
    }

    return new RatResult(RatResult.Status.SUCCESS, null);
  }

  public static boolean checkRepository(
      IdsAttestationType aType, AttestationResponse response, URI ttpUri) {
    List<Pcr> values = response.getPcrValuesList();
    try {
      ConnectorMessage msgRepo =
          RemoteAttestationHandler.readRepositoryResponse(
              ConnectorMessage.newBuilder()
                  .setId(privateID)
                  .setType(ConnectorMessage.Type.RAT_REPO_REQUEST)
                  .setAttestationRepositoryRequest(
                      AttestationRepositoryRequest.newBuilder()
                          .setAtype(aType)
                          .addAllPcrValues(values)
                          .build())
                  .build(),
              ttpUri.toURL());

      LOG.debug("//Q///////////////////////////////////////////////////////////////////////////");
      LOG.debug(response.toString());
      LOG.debug("//A///////////////////////////////////////////////////////////////////////////");
      LOG.debug(msgRepo.toString());
      LOG.debug("/////////////////////////////////////////////////////////////////////////////");

      return (msgRepo.getAttestationRepositoryResponse().getResult()
          && (msgRepo.getId() == privateID + 1)
          && (msgRepo.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE))
          && true); // TODO : signature check of repo answer ... !

    } catch (Exception ex) {
      lastError = "Exception: " + ex.getMessage();
      LOG.debug(
          "//Exception///////////////////////////////////////////////////////////////////////////");
      LOG.debug(lastError);
      LOG.debug(
          "//Exception///////////////////////////////////////////////////////////////////////////");
      return false;
    }
  }

  public static byte[] calculateHash(byte[] nonce, Certificate certificate) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(nonce);
      digest.update(certificate.getEncoded());
      return digest.digest();
    } catch (Exception e1) {
      LOG.error("Could not create hash of own nonce and local certificate", e1);
      return nonce;
    }
  }

  public boolean checkSignature(AttestationResponse response, byte[] hash) {
    byte[] byteSignature = response.getSignature().toByteArray();
    LOG.debug("signature: {}", ByteArrayUtil.toPrintableHexString(byteSignature));
    byte[] byteCert = response.getAikCertificate().toByteArray();
    LOG.debug("cert: {}", ByteArrayUtil.toPrintableHexString(byteCert));
    byte[] byteQuoted = response.getQuoted().toByteArray();

    if (byteCert.length == 0 || byteQuoted.length == 0) {
      LOG.debug("Response did not contain signature ");
      return false;
    }
    try {
      // construct a new TPM2B_PUBLIC from byteCert bytes
      TPM2B_PUBLIC tpm2bPublickey = new TPM2B_PUBLIC(byteCert);
      try {
        // and convert it into a java DER PublicKey key
        PublicKey publicKey = new PublicKeyConverter(tpm2bPublickey).getPublicKey();
        try {
          // construct a new TPMT_SIGNATURE from byteSignature bytes
          TPMT_SIGNATURE tpmtSignature = new TPMT_SIGNATURE(byteSignature);
          // check hash value (extra data) against expected hash
          TPMS_ATTEST tpmsAttest = new TPMS_ATTEST(byteQuoted);
          byte[] extraBytes = tpmsAttest.getExtraData().toBytes();
          if (!Arrays.equals(extraBytes, hash)) {
            LOG.warn("The hash (extra data) in TPMS_ATTEST structure is invalid!");
            return false;
          }

          // check signature depending on used hash algortihm
          final String sigAlg;
          switch (tpmtSignature.getSignature().getHashAlg()) {
            case TPM_ALG_SHA256:
              sigAlg = "SHA256withRSA";
              break;
            case TPM_ALG_SHA1:
              sigAlg = "SHA1withRSA";
              break;
            default:
              return false;
          }
          Signature sig = Signature.getInstance(sigAlg);
          sig.initVerify(publicKey);
          sig.update(byteQuoted);
          return sig.verify(tpmtSignature.getSignature().getSig());
        } catch (Exception ex) {
          LOG.warn("error: could not create a TPMT_SIGNATURE from bytes \""
              + ByteArrayUtil.toPrintableHexString(byteSignature)
              + "\":"
              + ex.getMessage(), ex);
          return false;
        }
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
        LOG.warn("error: could not convert from TPM2B_PUBLIC (\""
            + tpm2bPublickey
            + "\") to a PublicKey :"
            + ex.getMessage(), ex);
        return false;
      }
    } catch (Exception ex) {
      LOG.warn("error: could not create a TPM2B_PUBLIC (\""
          + ByteArrayUtil.toPrintableHexString(byteSignature)
          + "\") :"
          + ex.getMessage(), ex);
      return false;
    }
  }

  public static MessageLite sendError(Thread t, long id, String error) {
    if (t != null && t.isAlive()) {
      t.interrupt();
    }
    return ConnectorMessage.newBuilder()
        .setId(id)
        .setType(ConnectorMessage.Type.ERROR)
        .setError(Error.newBuilder().setErrorCode("").setErrorMessage(error).build())
        .build();
  }

  public static ConnectorMessage readRepositoryResponse(ConnectorMessage msg, URL adr)
      throws IOException, GeneralSecurityException {
    HttpsURLConnection urlc = (HttpsURLConnection) adr.openConnection();
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    urlc.setSSLSocketFactory(sslContext.getSocketFactory());
    urlc.setUseCaches(false);
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Accept", "application/x-protobuf");
    urlc.setRequestProperty("Content-Type", "application/x-protobuf");
    urlc.setRequestProperty("User-Agent", "IDS-Connector");
    urlc.setRequestProperty("Content-length", String.valueOf(msg.toByteArray().length));
    msg.writeTo(urlc.getOutputStream());
    return ConnectorMessage.newBuilder().mergeFrom(urlc.getInputStream()).build();
  }
}
