package de.fhg.aisec.ids.idscp2.default_drivers.rat.`null`

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners.RatVerifierFsmListener
import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A RatVerifier that exchanges messages with a remote RatProver dummy
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class NullRatVerifier(fsmListener: RatVerifierFsmListener) : RatVerifierDriver<Unit>(fsmListener) {
    private val queue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    override fun delegate(message: ByteArray) {
        queue.add(message)
        if (LOG.isDebugEnabled) {
            LOG.debug("Delegated to verifier")
        }
    }

    override fun run() {
        try {
            queue.take()
        } catch (e: InterruptedException) {
            if (running) {
                LOG.warn("NullRatVerifier failed")
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED)
            }
            return
        }
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG, "".toByteArray())
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK)
    }

    companion object {
        const val NULL_RAT_VERIFIER_ID = "NullRat"
        private val LOG = LoggerFactory.getLogger(NullRatVerifier::class.java)
    }
}