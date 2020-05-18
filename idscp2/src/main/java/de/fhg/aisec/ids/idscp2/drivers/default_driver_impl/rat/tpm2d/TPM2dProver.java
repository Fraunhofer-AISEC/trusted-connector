package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A TPM2d RatProver Driver implementation that proves its identity to a remote peer using TPM2d
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TPM2dProver extends RatProverDriver {
  private static final Logger LOG = LoggerFactory.getLogger(TPM2dProver.class);

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
  private TPM2dProverConfig config = new TPM2dProverConfig.Builder().build();

  public TPM2dProver(){
    super();
  }

  @Override
  public void setConfig(Object config) {
    if (config instanceof TPM2dProverConfig) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Set rat prover config");
      }
      this.config = (TPM2dProverConfig) config;
    } else {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Invalid prover config");
      }
    }
  }

  @Override
  public void delegate(byte[] message) {
    queue.add(message);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Delegated to prover");
    }
  }

  @Override
  public void run() {
    //TPM2d Challenge-Response Protocol

    // wait for RatChallenge from Verifier
    byte[] msg;
    try {
      msg = queue.take();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Prover receives new message");
      }
    } catch (InterruptedException e) {
      if (this.running){
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      }
      return;
    }

    // parse body to expected tpm2d message wrapper
    Tpm2dMessageWrapper tpm2dMessageWrapper;
    try {
      tpm2dMessageWrapper = Tpm2dMessageWrapper.parseFrom(msg);
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Cannot parse IdscpRatVerifier body", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if wrapper contains expected rat challenge
    if (!tpm2dMessageWrapper.hasRatChallenge()) {
      //unexpected message
      if (LOG.isWarnEnabled()) {
        LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatChallenge");
      }
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Get rat challenge from rat verifier");
    }
    Tpm2dRatChallenge challenge = tpm2dMessageWrapper.getRatChallenge();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Requesting attestation from TPM ...");
    }

    // hash
    byte[] hash = TPM2dHelper.calculateHash(challenge.getNonce().toByteArray(),
        config.getRemoteCertificate());

    // generate RemoteToTPM2dRequest
    RemoteToTpm2d tpmRequest = TPM2dMessageFactory.getRemoteToTPM2dMessage(
        challenge.getAtype(),
        hash,
        challenge.hasPcrIndices() ? challenge.getPcrIndices() : 0
    );

    // get TPM response
    Tpm2dToRemote tpmResponse;
    try {
      TPM2dSocket tpmSocket = new TPM2dSocket(config.getTpm2dHost());
      tpmResponse = tpmSocket.requestAttestation(tpmRequest);
    } catch (IOException e) {
      LOG.error("Cannot access TPM", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // create Tpm2dResponse
    byte[] response = TPM2dMessageFactory.getAttestationResponseMessage(tpmResponse)
        .toByteArray();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Send rat response to verifier");
    }
    fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG, response);

    // wait for result
    try {
      msg = queue.take();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Prover receives new message");
      }
    } catch (InterruptedException e) {
      if (this.running) {
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      }
      return;
    }

    // parse body to expected tpm2d message wrapper
    try {
      tpm2dMessageWrapper = Tpm2dMessageWrapper.parseFrom(msg);
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Cannot parse IdscpRatVerifier body", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if wrapper contains expected rat result
    if (!tpm2dMessageWrapper.hasRatResult()) {
      //unexpected message
      if (LOG.isWarnEnabled()) {
        LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatResult");
      }
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Get rat challenge from rat verifier");
    }
    Tpm2dRatResult result = tpm2dMessageWrapper.getRatResult();

    // notify fsm
    if (result.getResult()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Attestation succeed");
      }
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);
    } else {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Attestation failed");
      }
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
    }
  }
}
