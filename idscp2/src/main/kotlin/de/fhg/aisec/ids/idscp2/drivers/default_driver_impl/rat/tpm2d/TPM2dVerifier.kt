package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d

import com.google.protobuf.InvalidProtocolBufferException
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.Tpm2dMessageWrapper
import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.Tpm2dRatResponse
import org.slf4j.LoggerFactory
import tss.tpm.*
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A TPM2d RatVerifier driver that verifies the remote peer's identity using TPM2d
 */
class TPM2dVerifier(fsmListener: FsmListener) : RatVerifierDriver(fsmListener) {
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
    private var config = TPM2dVerifierConfig.Builder().build()
    override fun setConfig(config: Any) {
        if (config is TPM2dVerifierConfig) {
            LOG.debug("Set rat verifier config")
            this.config = config
        } else {
            LOG.warn("Invalid config")
        }
    }

    override fun delegate(message: ByteArray) {
        queue.add(message)
        LOG.debug("Delegated to Verifier")
    }

    override fun run() {
        //TPM2d Challenge-Response Protocol

        // create rat challenge with fresh nonce
        LOG.debug("Generate and send rat challenge for rat prover")
        val nonce = TPM2dHelper.generateNonce(20)

        // send challenge as RAT Verifier Message
        val ratChallenge = TPM2dMessageFactory.getAttestationChallengeMessage(
                nonce, config.expectedAType, config.expectedAttestationMask).toByteArray()
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG, ratChallenge)

        // wait for attestation response
        val msg: ByteArray
        try {
            msg = queue.take()
            LOG.debug("Verifier receives new message")
        } catch (e: InterruptedException) {
            if (running) {
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, ByteArray(0))
            }
            return
        }

        // parse body to expected tpm2d message wrapper
        val tpm2dMessageWrapper: Tpm2dMessageWrapper
        tpm2dMessageWrapper = try {
            Tpm2dMessageWrapper.parseFrom(msg)
        } catch (e: InvalidProtocolBufferException) {
            LOG.error("Cannot parse IdscpRatProver body", e)
            fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, ByteArray(0))
            return
        }

        // check if wrapper contains expected rat response
        if (!tpm2dMessageWrapper.hasRatResponse()) {
            //unexpected message
            LOG.warn("Unexpected message from RatProver: Expected Tpm2dRatResponse")
            fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, ByteArray(0))
            return
        }
        LOG.debug("Get rat response from remote prover")
        //validate rat response
        val resp = tpm2dMessageWrapper.ratResponse
        LOG.debug("Validate rat response: signature and rat repository checks")

        // validate signature
        var result = true
        val hash = TPM2dHelper.calculateHash(nonce, config.localCertificate)
        if (!checkSignature(resp, hash)) {
            result = false
            LOG.warn("Invalid rat signature")
        }

        // toDo check rat repo!!!!!!!!! --> Problems with current Rat Repo Protobuf format

        // create and send rat result
        LOG.debug("Send rat result to remote prover")
        val ratResult = TPM2dMessageFactory.getAttestationResultMessage(result).toByteArray()
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG, ratResult)

        // notify fsm about result
        if (result) {
            fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, ByteArray(0))
        } else {
            fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, ByteArray(0))
        }
    }

    private fun checkSignature(response: Tpm2dRatResponse, hash: ByteArray): Boolean {
        val byteSignature = response.signature.toByteArray()
        val byteCert = response.certificate.toByteArray()
        val byteQuoted = response.quoted.toByteArray()
        if (LOG.isDebugEnabled) {
            LOG.debug("signature: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteSignature))
            LOG.debug("cert: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteCert))
            LOG.debug("quoted: {}", TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteQuoted))
        }
        if (byteSignature.isEmpty() || byteCert.isEmpty() || byteQuoted.isEmpty()) {
            LOG.warn("Some required part (signature, cert or quoted) is empty!")
            return false
        }
        return try {
            val certFactory = CertificateFactory.getInstance("X.509")

            // Load trust anchor certificate
            var rootCertificate: X509Certificate
            val rootCertPath = FileSystems.getDefault().getPath("etc", "rootca-cert.pem")
            try {
                Files.newBufferedReader(rootCertPath, StandardCharsets.US_ASCII).use { reader ->
                    val builder = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        if (!line.startsWith("-")) {
                            builder.append(line.trim { it <= ' ' })
                        }
                        line = reader.readLine()
                    }
                    val rootCertBytes = Base64.getDecoder().decode(builder.toString())
                    rootCertificate = certFactory.generateCertificate(ByteArrayInputStream(rootCertBytes)) as X509Certificate
                }
            } catch (e: Exception) {
                LOG.error("Error parsing root certificate", e)
                return false
            }

            // Create X509Certificate instance from certBytes
            val certificate = certFactory.generateCertificate(ByteArrayInputStream(byteCert)) as X509Certificate
            // Verify the TPM certificate
            try {
                certificate.verify(rootCertificate.publicKey)
            } catch (e: Exception) {
                LOG.error("TPM certificate is invalid", e)
                return false
            }

            // Construct a new TPMT_SIGNATURE instance from byteSignature bytes
            val tpmtSignature: TPMT_SIGNATURE
            tpmtSignature = try {
                TPMT_SIGNATURE.fromTpm(byteSignature)
            } catch (ex: Exception) {
                LOG.warn("""
                Could not create a TPMT_SIGNATURE from bytes:
                ${TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteSignature)}
                """.trimIndent(),
                        ex)
                return false
            }

            // Construct a new TPMS_ATTEST instance from byteQuoted bytes
            val tpmsAttest: TPMS_ATTEST
            tpmsAttest = try {
                TPMS_ATTEST.fromTpm(byteQuoted)
            } catch (ex: Exception) {
                LOG.warn("""
                Could not create a TPMS_ATTEST from bytes:
                ${TPM2dHelper.ByteArrayUtil.toPrintableHexString(byteQuoted)}
                """.trimIndent(),
                        ex)
                return false
            }

            // check hash value (extra data) against expected hash
            val extraBytes = tpmsAttest.extraData
            if (!Arrays.equals(extraBytes, hash)) {
                if (LOG.isWarnEnabled) {
                    LOG.warn("""
                    The hash (extra data) in TPMS_ATTEST structure is invalid!
                    extra data: {}
                    hash: {}
                    """.trimIndent(),
                            TPM2dHelper.ByteArrayUtil.toPrintableHexString(extraBytes),
                            TPM2dHelper.ByteArrayUtil.toPrintableHexString(hash))
                }
                return false
            }

            // Check signature of attestation
            val tpmSigAlg = tpmtSignature.GetUnionSelector_signature()
            val tpmSigHashAlg: Int
            val tpmSig: ByteArray
            if (tpmSigAlg == TPM_ALG_ID.RSAPSS.toInt()) {
                tpmSigHashAlg = (tpmtSignature.signature as TPMS_SIGNATURE_RSAPSS).hash.toInt()
                tpmSig = (tpmtSignature.signature as TPMS_SIGNATURE_RSAPSS).sig
            } else if (tpmSigAlg == TPM_ALG_ID.RSASSA.toInt()) {
                tpmSigHashAlg = (tpmtSignature.signature as TPMS_SIGNATURE_RSASSA).hash.toInt()
                tpmSig = (tpmtSignature.signature as TPMS_SIGNATURE_RSASSA).sig
            } else {
                throw Exception(
                        "Unknown or unimplemented signature scheme: " + tpmtSignature.signature.javaClass)
            }
            if (tpmSigHashAlg != TPM_ALG_ID.SHA256.toInt()) {
                throw Exception("Only SHA256withRSA TPM signature hash algorithm is allowed!")
            }
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(certificate.publicKey)
            sig.update(byteQuoted)
            val result = sig.verify(tpmSig)
            if (!result && LOG.isWarnEnabled) {
                LOG.warn("Attestation signature invalid!")
            }
            result
        } catch (ex: Exception) {
            LOG.warn("Error during attestation validation", ex)
            false
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TPM2dVerifier::class.java)
    }
}