package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dMessageWrapper;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dRatResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tss.tpm.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A TPM2d RatVerifier driver that verifies the remote peer's identity using TPM2d
 */
public class TPM2dVerifier extends RatVerifierDriver {
  private static final Logger LOG = LoggerFactory.getLogger(TPM2dVerifier.class);

  /*
   * ******************* Protocol *******************
   *
   * Verifier: (Challenger)
   * -------------------------
   * Generate NonceV
   * create RatChallenge (NonceV, aType, pcr_mask)
   * -------------------------
   *
   * Prover: (Responder)
   * -------------------------
   * get RatChallenge (NonceV, aType, pcr_mask)
   * hash = calculateHash(nonceV, certV)
   * req = generate RemoteToTPM2dRequest(hash, aType, pcr_mask)
   * response = TPM2dToRemote = tpmSocket.attestationRequest(req)
   * create AttestationResponse from tpm response
   * -------------------------
   *
   * Verifier: (Responder)
   * -------------------------
   * get AttestationResponse
   * hash = calculateHash(nonceV, certV)
   * check signature(response, hash)
   * check repo(aType, response, ttpUri)
   * create RatResult
   * -------------------------
   *
   * Prover: (Requester)
   * -------------------------
   * get AttestationResult
   * -------------------------
   *
   */

  private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
  private Tpm2dVerifierConfig config = new Tpm2dVerifierConfig.Builder().build();

  public TPM2dVerifier(){
    super();
  }

  @Override
  public void setConfig(Object config) {
    if (config instanceof Tpm2dVerifierConfig) {
      LOG.debug("Set rat verifier config");
      this.config = (Tpm2dVerifierConfig) config;
    } else {
      LOG.warn("Invalid config");
    }
  }

  @Override
  public void delegate(byte[] message) {
    queue.add(message);
    LOG.debug("Delegated to Verifier");
  }

  @Override
  public void run(){
    //TPM2d Challenge-Response Protocol

    // create rat challenge with fresh nonce
    LOG.debug("Generate and send rat challenge for rat prover");
    byte[] nonce = TPM2dHelper.generateNonce(20);

    // send challenge as RAT Verifier Message
    byte[] ratChallenge = TpmMessageFactory.getAttestationChallengeMessage(
        nonce, config.getExpectedAType(), config.getExpectedAttestationMask()).toByteArray();

    fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG, ratChallenge);

    // wait for attestation response
    byte[] msg;
    try {
      msg = queue.take();
      LOG.debug("Verifier receives new message");
    } catch (InterruptedException e) {
      if (this.running) {
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
      }
      return;
    }

    // parse body to expected tpm2d message wrapper
    Tpm2dMessageWrapper tpm2dMessageWrapper;
    try {
      tpm2dMessageWrapper = Tpm2dMessageWrapper.parseFrom(msg);
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Cannot parse IdscpRatProver body", e);
      fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
      return;
    }

    // check if wrapper contains expected rat response
    if (!tpm2dMessageWrapper.hasRatResponse()) {
      //unexpected message
      LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatResponse");
      fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
      return;
    }

    LOG.debug("Get rat response from remote prover");
    //validate rat response
    Tpm2dRatResponse resp = tpm2dMessageWrapper.getRatResponse();

    LOG.debug("Validate rat response: signature and rat repository checks");

    // validate signature
    boolean result = true;
    byte[] hash = TPM2dHelper.calculateHash(nonce, config.getLocalCertificate());

    if (!checkSignature(resp, hash)) {
      result = false;
      LOG.warn("Invalid rat signature");
    }

    // toDo check rat repo!!!!!!!!! --> Problems with current Rat Repo Protobuf format

    // create and send rat result
    LOG.debug("Send rat result to remote prover");
    byte[] ratResult = TpmMessageFactory.getAttestationResultMessage(result).toByteArray();
    fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG, ratResult);

    // notify fsm about result
    if (result) {
      fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, null);
    } else {
      fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
    }
  }

  private boolean checkSignature(@NonNull Tpm2dRatResponse response, byte[] hash) {
    byte[] byteSignature = response.getSignature().toByteArray();
    byte[] byteCert = response.getCertificate().toByteArray();
    byte[] byteQuoted = response.getQuoted().toByteArray();

    if (LOG.isDebugEnabled()) {
      LOG.debug("signature: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteSignature));
      LOG.debug("cert: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteCert));
      LOG.debug("quoted: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteQuoted));
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
      try (BufferedReader reader =
          Files.newBufferedReader(rootCertPath, StandardCharsets.US_ASCII)) {
        StringBuilder builder = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
          if (!line.startsWith("-")) {
            builder.append(line.trim());
          }
        }
        byte[] rootCertBytes = Base64.getDecoder().decode(builder.toString());
        rootCertificate =
            (X509Certificate)
                certFactory.generateCertificate(new ByteArrayInputStream(rootCertBytes));
      } catch (Exception e) {
        LOG.error("Error parsing root certificate", e);
        return false;
      }

      // Create X509Certificate instance from certBytes
      final X509Certificate certificate =
          (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(byteCert));
      // Verify the TPM certificate
      try {
        certificate.verify(rootCertificate.getPublicKey());
      } catch (Exception e) {
        LOG.error("TPM certificate is invalid", e);
        return false;
      }

      // Construct a new TPMT_SIGNATURE instance from byteSignature bytes
      final TPMT_SIGNATURE tpmtSignature;
      try {
        tpmtSignature = TPMT_SIGNATURE.fromTpm(byteSignature);
      } catch (Exception ex) {
        LOG.warn(
            "Could not create a TPMT_SIGNATURE from bytes:\n"
                + TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteSignature),
            ex);
        return false;
      }

      // Construct a new TPMS_ATTEST instance from byteQuoted bytes
      final TPMS_ATTEST tpmsAttest;
      try {
        tpmsAttest = TPMS_ATTEST.fromTpm(byteQuoted);
      } catch (Exception ex) {
        LOG.warn(
            "Could not create a TPMS_ATTEST from bytes:\n"
                + TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteQuoted),
            ex);
        return false;
      }

      // check hash value (extra data) against expected hash
      byte[] extraBytes = tpmsAttest.extraData;
      if (!Arrays.equals(extraBytes, hash)) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(
              "The hash (extra data) in TPMS_ATTEST structure is invalid!"
                  + "\nextra data: {}\nhash: {}",
              TPM2dHelper.ByteArrayUtil.toPrintableHexString(extraBytes),
              TPM2dHelper.ByteArrayUtil.toPrintableHexString(hash));
        }
        return false;
      }

      // Check signature of attestation
      final int tpmSigAlg = tpmtSignature.GetUnionSelector_signature();
      final int tpmSigHashAlg;
      final byte[] tpmSig;
      if (tpmSigAlg == TPM_ALG_ID.RSAPSS.toInt()) {
        tpmSigHashAlg = ((TPMS_SIGNATURE_RSAPSS) tpmtSignature.signature).hash.toInt();
        tpmSig = ((TPMS_SIGNATURE_RSAPSS) tpmtSignature.signature).sig;
      } else if (tpmSigAlg == TPM_ALG_ID.RSASSA.toInt()) {
        tpmSigHashAlg = ((TPMS_SIGNATURE_RSASSA) tpmtSignature.signature).hash.toInt();
        tpmSig = ((TPMS_SIGNATURE_RSASSA) tpmtSignature.signature).sig;
      } else {
        throw new Exception(
            "Unknown or unimplemented signature scheme: " + tpmtSignature.signature.getClass());
      }
      if (tpmSigHashAlg != TPM_ALG_ID.SHA256.toInt()) {
        throw new Exception("Only SHA256withRSA TPM signature hash algorithm is allowed!");
      }
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initVerify(certificate.getPublicKey());
      sig.update(byteQuoted);
      boolean result = sig.verify(tpmSig);
      if (!result && LOG.isWarnEnabled()) {
        LOG.warn("Attestation signature invalid!");
      }
      return result;
    } catch (Exception ex) {
      LOG.warn("Error during attestation validation", ex);
      return false;
    }
  }
}
