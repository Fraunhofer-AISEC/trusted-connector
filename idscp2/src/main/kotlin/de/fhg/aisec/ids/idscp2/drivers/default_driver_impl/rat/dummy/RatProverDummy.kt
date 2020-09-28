package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A RatProver dummy that exchanges rat messages with a remote RatVerifier
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class RatProverDummy(fsmListener: FsmListener) : RatProverDriver(fsmListener) {
    private val queue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    override fun delegate(message: ByteArray) {
        queue.add(message)
        if (LOG.isDebugEnabled) {
            LOG.debug("Delegated to prover")
        }
    }

    override fun run() {
        var countDown = 2
        while (running) {
            try {
                sleep(1000)
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG,
                        "test".toByteArray())
                if (LOG.isDebugEnabled) {
                    LOG.debug("Prover waits")
                }
                queue.take()
                if (LOG.isDebugEnabled) {
                    LOG.debug("Prover receives, send something")
                }
                if (--countDown == 0) break
            } catch (e: InterruptedException) {
                if (running) {
                    fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED)
                }
                return
            }
        }
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RatProverDummy::class.java)
    }
}