package de.fhg.aisec.ids.idscp2.idscp_core.drivers

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import org.slf4j.LoggerFactory

/**
 * An abstract RatProverDriver class that creates a rat prover driver thread and proves itself to
 * the peer connector using remote attestation
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
abstract class RatProverDriver<in PC>(protected val fsmListener: FsmListener) : Thread() {
    protected var running = true

    /*
     * Delegate an IDSCP2 message to the RatProver driver
     */
    open fun delegate(message: ByteArray) {}

    /*
     * Terminate and cancel the RatProver driver
     */
    fun terminate() {
        running = false
        interrupt()
    }

    open fun setConfig(config: PC) {
        LOG.warn("Method 'setConfig' for RatProverDriver is not implemented")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RatProverDriver::class.java)
    }
}