package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureChannelEndpoint
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FsmListener
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * A secureChannel which is the secure underlying basis of the IDSCP2 protocol,
 * that implements a secureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class SecureChannel(private val endpoint: SecureChannelEndpoint) : SecureChannelListener {
    private val fsmPromise = CompletableFuture<FsmListener>()

    /*
     * close the secure channel forever
     */
    fun close() {
        endpoint.close()
    }

    /*
     * Send data via the secure channel endpoint to the peer connector
     *
     * return true if the data has been sent successfully, else false
     */
    fun send(msg: ByteArray): Boolean {
        if (LOG.isTraceEnabled) {
            LOG.trace("Send message via secure channel")
        }
        return endpoint.send(msg)
    }

    override fun onMessage(data: ByteArray) {
        if (LOG.isTraceEnabled) {
            LOG.trace("New raw data has been received via the secure channel")
        }
        fsmPromise.thenAccept { fsmListener: FsmListener -> fsmListener.onMessage(data) }
    }

    override fun onError(t: Throwable) {
        // Tell fsm an error occurred in secure channel
        fsmPromise.thenAccept { fsmListener: FsmListener -> fsmListener.onError(t) }
    }

    override fun onClose() {
        // Tell fsm secure channel received EOF
        fsmPromise.thenAccept { obj: FsmListener -> obj.onClose() }
    }

    val isConnected: Boolean
        get() = endpoint.isConnected

    /*
     * set the corresponding finite state machine
     */
    fun setFsm(fsm: FsmListener) {
        fsmPromise.complete(fsm)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SecureChannel::class.java)
    }
}