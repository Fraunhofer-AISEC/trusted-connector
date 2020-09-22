package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A RatVerifier dummy that exchanges messages with a remote RatProver dummy
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class RatVerifierDummy(fsmListener: FsmListener) : RatVerifierDriver(fsmListener) {
    private val queue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    override fun delegate(message: ByteArray) {
        queue.add(message)
        if (LOG.isDebugEnabled) {
            LOG.debug("Delegated to Verifier")
        }
    }

    override fun run() {
        var countDown = 2
        while (running) {
            try {
                sleep(1000)
                if (LOG.isDebugEnabled) {
                    LOG.debug("Verifier waits")
                }
                queue.take()
                if (LOG.isDebugEnabled) {
                    LOG.debug("Verifier receives, send something")
                }
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG,
                        "test".toByteArray())
                if (--countDown == 0) break
            } catch (e: InterruptedException) {
                if (running) {
                    fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, ByteArray(0))
                }
                return
            }
        }
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, ByteArray(0))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RatVerifierDummy::class.java)
    }
}