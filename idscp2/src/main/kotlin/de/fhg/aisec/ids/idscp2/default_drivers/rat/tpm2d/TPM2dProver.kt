package de.fhg.aisec.ids.idscp2.default_drivers.rat.tpm2d

import com.google.protobuf.InvalidProtocolBufferException
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.RatProverDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners.RatProverFsmListener
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.Tpm2dMessageWrapper
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A TPM2d RatProver Driver implementation that proves its identity to a remote peer using TPM2d
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TPM2dProver(fsmListener: RatProverFsmListener) : RatProverDriver<TPM2dProverConfig>(fsmListener) {
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
    private val queue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    private var config = TPM2dProverConfig.Builder().build()
    override fun setConfig(config: TPM2dProverConfig) {
        if (LOG.isDebugEnabled) {
            LOG.debug("Set rat prover config")
        }
        this.config = config
    }

    override fun delegate(message: ByteArray) {
        queue.add(message)
        if (LOG.isDebugEnabled) {
            LOG.debug("Delegated to prover")
        }
    }

    override fun run() {
        //TPM2d Challenge-Response Protocol

        // wait for RatChallenge from Verifier
        var msg: ByteArray?
        try {
            msg = queue.take()
            if (LOG.isDebugEnabled) {
                LOG.debug("Prover receives new message")
            }
        } catch (e: InterruptedException) {
            if (running) {
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            }
            return
        }

        // parse body to expected tpm2d message wrapper
        var tpm2dMessageWrapper: Tpm2dMessageWrapper = try {
            Tpm2dMessageWrapper.parseFrom(msg)
        } catch (e: InvalidProtocolBufferException) {
            LOG.error("Cannot parse IdscpRatVerifier body", e)
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            return
        }

        // check if wrapper contains expected rat challenge
        if (!tpm2dMessageWrapper.hasRatChallenge()) {
            //unexpected message
            if (LOG.isWarnEnabled) {
                LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatChallenge")
            }
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            return
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Get rat challenge from rat verifier")
        }
        val challenge = tpm2dMessageWrapper.ratChallenge
        if (LOG.isDebugEnabled) {
            LOG.debug("Requesting attestation from TPM ...")
        }

        // hash
        val hash = TPM2dHelper.calculateHash(challenge.nonce.toByteArray(),
                config.remoteCertificate)

        // generate RemoteToTPM2dRequest
        val tpmRequest = TPM2dMessageFactory.getRemoteToTPM2dMessage(
                challenge.atype,
                hash,
                if (challenge.hasPcrIndices()) challenge.pcrIndices else 0
        )

        // get TPM response
        val tpmResponse: Tpm2dAttestation.Tpm2dToRemote = try {
            val tpmSocket = TPM2dSocket(config.tpm2dHost)
            tpmSocket.requestAttestation(tpmRequest)
        } catch (e: IOException) {
            LOG.error("Cannot access TPM", e)
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            return
        }

        // create Tpm2dResponse
        val response = TPM2dMessageFactory.getAttestationResponseMessage(tpmResponse)
                .toByteArray()
        if (LOG.isDebugEnabled) {
            LOG.debug("Send rat response to verifier")
        }
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG, response)

        // wait for result
        try {
            msg = queue.take()
            if (LOG.isDebugEnabled) {
                LOG.debug("Prover receives new message")
            }
        } catch (e: InterruptedException) {
            if (running) {
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            }
            return
        }

        // parse body to expected tpm2d message wrapper
        tpm2dMessageWrapper = try {
            Tpm2dMessageWrapper.parseFrom(msg)
        } catch (e: InvalidProtocolBufferException) {
            LOG.error("Cannot parse IdscpRatVerifier body", e)
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            return
        }

        // check if wrapper contains expected rat result
        if (!tpm2dMessageWrapper.hasRatResult()) {
            //unexpected message
            if (LOG.isWarnEnabled) {
                LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatResult")
            }
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            return
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Get rat challenge from rat verifier")
        }
        val result = tpm2dMessageWrapper.ratResult

        // notify fsm
        if (result.result) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Attestation succeed")
            }
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK)
        } else {
            if (LOG.isWarnEnabled) {
                LOG.warn("Attestation failed")
            }
            fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
        }
    }

    companion object {
        const val TPM_RAT_PROVER_ID = "TPM2d"
        private val LOG = LoggerFactory.getLogger(TPM2dProver::class.java)
    }
}