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
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.aisec.ids.messages.Idscp.*;
import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class RemoteAttestationHandler {
  public static final String CONTROL_SOCKET = "/var/run/tpm2d/control.sock";
  protected static final Logger LOG =
      LoggerFactory.getLogger(RemoteAttestationClientHandler.class);
  static String lastError = "";
  // used to count messages between ids connector and attestation repository
  private static long privateID = new java.util.Random().nextLong();
  boolean mySuccess = false;
  boolean yourSuccess = false;
  Tpm2dSocket tpm2dSocket;

  RemoteAttestationHandler() {
      // Tpm2dSocket used to communicate with local TPM2d
      try {
          String host = System.getenv("TPM_HOST") != null ? System.getenv("TPM_HOST") : "localhost";
          tpm2dSocket = new Tpm2dSocket(host);
      } catch (IOException e) {
          lastError = "Could not create Tpm2dSocket. No TPM present?";
          LOG.warn(lastError);
      }
  }

  public RatResult handleAttestationResult(@NonNull AttestationResult result) {
    this.yourSuccess = result.getResult();

    LOG.debug("your success: {}    my success: {}", this.yourSuccess, this.mySuccess);
    if (!this.mySuccess) {
      return new RatResult(RatResult.Status.FAILED, "Could not verify");
    }

    if (!this.yourSuccess) {
      return new RatResult(RatResult.Status.FAILED, "Remote party did not verify successfully");
    }

    return new RatResult(RatResult.Status.SUCCESS, null);
  }

  static boolean checkRepository(
          @Nullable IdsAttestationType aType, @Nullable AttestationResponse response, @Nullable URI ttpUri) {
    if (aType == null || response == null || ttpUri == null) {
      return false;
    }

    List<Pcr> values = response.getPcrValuesList();
    try {
      @SuppressWarnings("null")
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

      // TODO : signature check of repo answer ... !
      return (msgRepo.getAttestationRepositoryResponse().getResult()
          && (msgRepo.getId() == privateID + 1)
          && (msgRepo.getType().equals(ConnectorMessage.Type.RAT_REPO_RESPONSE)));

    } catch (Exception ex) {
      lastError = "Exception: " + ex.getMessage();
      LOG.error("Exception in checkRepository(): ", ex);
      return false;
    }
  }

  /**
   * Calculate SHA-1 hash of (nonce|certificate).
   *
   * @param nonce The plain, initial nonce
   * @param certificate The certificate to hash-combine with the nonce
   * @return The new nonce, updated with the given certificate using SHA-1
   */
  static byte[] calculateHash(byte[] nonce, @Nullable Certificate certificate) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(nonce);
      if (certificate != null) {
    	  digest.update(certificate.getEncoded());
      } else {
    	  LOG.warn("No client certificate available. Cannot bind nonce to public key to prevent masquerading attack. TLS misconfiguration!");
      }
      return digest.digest();
    } catch (Exception e1) {
      LOG.error("Could not create hash of own nonce and local certificate", e1);
      return nonce;
    }
  }

  boolean checkSignature(@NonNull AttestationResponse response, byte[] hash) {
    byte[] byteSignature = response.getSignature().toByteArray();
    byte[] byteCert = response.getCertificate().toByteArray();
    byte[] byteQuoted = response.getQuoted().toByteArray();
    if (LOG.isDebugEnabled()) {
      LOG.debug("signature: {}", ByteArrayUtil.toPrintableHexString(byteSignature));
      LOG.debug("cert: {}", ByteArrayUtil.toPrintableHexString(byteCert));
      LOG.debug("quoted: {}", ByteArrayUtil.toPrintableHexString(byteQuoted));
    }

    if (byteSignature.length == 0 || byteCert.length == 0 || byteQuoted.length == 0) {
      LOG.warn("Some required part (signature, cert or quoted) is empty!");
      return false;
    }

    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

      // Load trust anchor certificate
      final X509Certificate rootCertificate;
      Path rootCertPath = FileSystems.getDefault().getPath("etc", "rootca-cert.pem");
      try (BufferedReader reader = Files.newBufferedReader(rootCertPath, StandardCharsets.US_ASCII)) {
        StringBuilder builder = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
          if (!line.startsWith("-")) {
            builder.append(line.trim());
          }
        }
        byte[] rootCertBytes = Base64.getDecoder().decode(builder.toString());
        rootCertificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(rootCertBytes));
      } catch (Exception e) {
        LOG.error("Error parsing root certificate", e);
        return false;
      }

      // Create X509Certificate instance from certBytes
      final X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
              new ByteArrayInputStream(byteCert));
      // TODO: Reactivate immediately when tpm certificate provisioning issue is solved!!!
      // Verify the TPM certificate
//      try {
//        certificate.verify(rootCertificate.getPublicKey());
//      } catch (Exception e) {
//        LOG.error("TPM certificate is invalid", e);
//        return false;
//      }

      // Construct a new TPMT_SIGNATURE instance from byteSignature bytes
      final TPMT_SIGNATURE tpmtSignature;
      try {
        tpmtSignature = new TPMT_SIGNATURE(byteSignature);
      } catch (Exception ex) {
        LOG.warn("Could not create a TPMT_SIGNATURE from bytes:\n" + ByteArrayUtil.toPrintableHexString(byteSignature),
                ex);
        return false;
      }

      // Construct a new TPMS_ATTEST instance from byteQuoted bytes
      final TPMS_ATTEST tpmsAttest;
      try {
        tpmsAttest = new TPMS_ATTEST(byteQuoted);
      } catch (Exception ex) {
        LOG.warn("Could not create a TPMS_ATTEST from bytes:\n" + ByteArrayUtil.toPrintableHexString(byteQuoted), ex);
        return false;
      }

      // check hash value (extra data) against expected hash
      byte[] extraBytes = tpmsAttest.getExtraData().getBuffer();
      if (!Arrays.equals(extraBytes, hash)) {
        if (LOG.isWarnEnabled()) {
          LOG.warn("The hash (extra data) in TPMS_ATTEST structure is invalid!"
                          + "\nextra data: {}\nhash: {}",
                  ByteArrayUtil.toPrintableHexString(extraBytes),
                  ByteArrayUtil.toPrintableHexString(hash));
        }
        return false;
      }

      // TODO: Reactivate immediately when tpm certificate provisioning issue is solved!!!
      // Check signature of attestation
//      final String sigAlg;
//      if (tpmtSignature.getSignature().getHashAlg() == TPM_ALG_ID.ALG_ID.TPM_ALG_SHA256) {
//        sigAlg = "SHA256withRSA";
//      } else {
//        LOG.warn("Only SHA256withRSA TPM signature algorithm is allowed!");
//        return false;
//      }
//      Signature sig = Signature.getInstance(sigAlg);
//      sig.initVerify(certificate.getPublicKey());
//      sig.update(byteQuoted);
//      return sig.verify(tpmtSignature.getSignature().getSig());
      return true;
    } catch (Exception ex) {
      LOG.warn("Error during attestation validation", ex);
      return false;
    }
  }

  static MessageLite sendError(long id, @Nullable String error) {
    if (error == null) {
    	error = "";
    }
    return ConnectorMessage.newBuilder()
        .setId(id)
        .setType(ConnectorMessage.Type.ERROR)
        .setError(Error.newBuilder().setErrorCode("").setErrorMessage(error).build())
        .build();
  }

  private static ConnectorMessage readRepositoryResponse(@NonNull ConnectorMessage msg, URL adr)
      throws IOException, GeneralSecurityException {
    HttpsURLConnection urlc = (HttpsURLConnection) adr.openConnection();
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null, null, null);
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
