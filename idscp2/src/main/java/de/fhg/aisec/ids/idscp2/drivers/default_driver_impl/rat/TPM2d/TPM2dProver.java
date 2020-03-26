package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;

import de.fhg.aisec.ids.messages.Tpm2dAttestation.RemoteToTpm2d;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dMessageWrapper;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dRatChallenge;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dRatResult;
import de.fhg.aisec.ids.messages.Tpm2dAttestation.Tpm2dToRemote;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TPM2dProver extends RatProverDriver {
  private static final Logger LOG = LoggerFactory.getLogger(TPM2dProver.class);

  private BlockingQueue<IdscpMessage> queue = new LinkedBlockingQueue<>();

  public TPM2dProver(){
    super();
  }

  @Override
  public void delegate(IdscpMessage message) {
    queue.add(message);
    LOG.debug("Delegated to prover");
  }

  @Override
  public void run() {
    //TPM2d Challenge-Response Protocol

    // wait for RatChallenge from Verifier
    IdscpMessage msg;
    try {
      msg = queue.take();
      LOG.debug("Prover receives new message");
    } catch (InterruptedException e) {
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if message is from rat verifier
    if (!msg.hasIdscpRatVerifier()) {
      //unexpected message
      LOG.warn("Unexpected message from FSM: Expected IdscpRatProver");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // parse body to expected tpm2d message wrapper
    Tpm2dMessageWrapper tpm2dMessageWrapper;
    try {
      tpm2dMessageWrapper = Tpm2dMessageWrapper.parseFrom(msg.getIdscpRatVerifier().getData());
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Cannot parse IdscpRatVerifier body", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if wrapper contains expected rat challenge
    if (!tpm2dMessageWrapper.hasRatChallenge()) {
      //unexpected message
      LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatChallenge");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    LOG.debug("Get rat challenge from rat verifier");
    Tpm2dRatChallenge challenge = tpm2dMessageWrapper.getRatChallenge();

    LOG.debug("Requesting attestation from TPM ...");
    // hash //toDo add remote certificate
    byte[] hash = TPM2dHelper.calculateHash(challenge.getNonce().toByteArray(), null);

    // generate RemoteToTPM2dRequest
    RemoteToTpm2d tpmRequest = TpmMessageFactory.getRemoteToTPM2dMessage(
        challenge.getAtype(),
        hash,
        challenge.hasPcrIndices() ? challenge.getPcrIndices() : 0 //toDo 0 okay?
    );

    // get TPM response
    Tpm2dToRemote tpmResponse;
    try {
      TPM2dSocket tpmSocket = new TPM2dSocket(""); //toDo add host
      tpmResponse = tpmSocket.requestAttestation(tpmRequest);
    } catch (IOException e) {
      LOG.error("Cannot access TPM", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // create Tpm2dResponse
    byte[] response = TpmMessageFactory.getAttestationResponseMessage(tpmResponse)
        .toByteArray();

    LOG.debug("Send rat response to verifier");
    fsmListener.onRatProverMessage(
        InternalControlMessage.RAT_PROVER_MSG,
        IdscpMessageFactory.getIdscpRatProverMessage(response)
    );

    // wait for result
    try {
      msg = queue.take();
      LOG.debug("Prover receives new message");
    } catch (InterruptedException e) {
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if message is from rat verifier
    if (!msg.hasIdscpRatVerifier()) {
      //unexpected message
      LOG.warn("Unexpected message from FSM: Expected IdscpRatProver");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // parse body to expected tpm2d message wrapper
    try {
      tpm2dMessageWrapper = Tpm2dMessageWrapper.parseFrom(msg.getIdscpRatVerifier().getData());
    } catch (InvalidProtocolBufferException e) {
      LOG.error("Cannot parse IdscpRatVerifier body", e);
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    // check if wrapper contains expected rat result
    if (!tpm2dMessageWrapper.hasRatResult()) {
      //unexpected message
      LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatResult");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
      return;
    }

    LOG.debug("Get rat challenge from rat verifier");
    Tpm2dRatResult result = tpm2dMessageWrapper.getRatResult();

    // notify fsm
    if (result.getResult()) {
      LOG.debug("Attestation succeed");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);
    } else {
      LOG.warn("Attestation failed");
      fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
    }
  }
}
