package de.fhg.aisec.ids.idscp2.default_drivers.rat.`null`

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.RatProverDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners.RatProverFsmListener
import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A RatProver that exchanges rat messages with a remote RatVerifier
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class NullRatProver(fsmListener: RatProverFsmListener) : RatProverDriver<Unit>(fsmListener) {
    private val queue: BlockingQueue<ByteArray> = LinkedBlockingQueue()

    override fun delegate(message: ByteArray) {
        queue.add(message)
        if (LOG.isDebugEnabled) {
            LOG.debug("Delegated to prover")
        }
    }

    override fun run() {
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG, "".toByteArray())
        try {
            queue.take()
        } catch (e: InterruptedException) {
            if (running) {
                LOG.warn("NullRatProver failed")
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
            }
            return
        }
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK)
    }

    companion object {
        const val NULL_RAT_PROVER_ID = "NullRat"
        private val LOG = LoggerFactory.getLogger(NullRatProver::class.java)
    }
}