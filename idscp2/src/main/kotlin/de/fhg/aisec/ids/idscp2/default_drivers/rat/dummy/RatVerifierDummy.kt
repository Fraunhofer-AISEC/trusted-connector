package de.fhg.aisec.ids.idscp2.default_drivers.rat.dummy

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.RatVerifierDriver
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
class RatVerifierDummy(fsmListener: FsmListener) : RatVerifierDriver<Unit>(fsmListener) {
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
                    fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED)
                }
                return
            }
        }
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK)
    }

    companion object {
        val RAT_VERIFIER_DUMMY_ID = "Dummy"
        private val LOG = LoggerFactory.getLogger(RatVerifierDummy::class.java)
    }
}