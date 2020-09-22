package de.fhg.aisec.ids.idscp2.drivers.interfaces

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import org.slf4j.LoggerFactory

/**
 * An abstract RatVerifierDriver class that creates a rat verifier driver thread and verifier the
 * peer connector using remote attestation
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
abstract class RatVerifierDriver(protected val fsmListener: FsmListener) : Thread() {
    protected var running = true

    /*
     * Delegate the IDSCP2 message to the RatVerifier driver
     */
    open fun delegate(message: ByteArray) {}

    /*
     * Terminate and cancel the RatVerifier driver
     */
    fun terminate() {
        running = false
        interrupt()
    }

    open fun setConfig(config: Any) {
        LOG.warn("Method 'setConfig' for RatVerifierDriver is not implemented")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RatVerifierDriver::class.java)
    }
}