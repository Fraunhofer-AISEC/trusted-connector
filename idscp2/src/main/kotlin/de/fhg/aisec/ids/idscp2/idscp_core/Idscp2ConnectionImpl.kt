package de.fhg.aisec.ids.idscp2.idscp_core

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * The IDSCP2 Connection class holds connections between connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
class Idscp2ConnectionImpl(secureChannel: SecureChannel,
                           settings: Idscp2Settings,
                           dapsDriver: DapsDriver): Idscp2Connection {
    private val fsm: FSM = FSM(
            this,
            secureChannel,
            dapsDriver,
            settings.supportedAttestation.ratMechanisms,
            settings.expectedAttestation.ratMechanisms,
            settings.ratTimeoutDelay)
    override val id: String = UUID.randomUUID().toString()
    private val connectionListeners = Collections.synchronizedSet(HashSet<Idscp2ConnectionListener>())
    private val messageListeners = Collections.synchronizedSet(HashSet<Idscp2MessageListener>())
    private val messageLatch = FastLatch()
    override fun unlockMessaging() {
        messageLatch.unlock()
    }

    /**
     * Close the idscp connection
     */
    override fun close() {
        if (LOG.isDebugEnabled) {
            LOG.debug("Closing connection {}...", id)
        }
        fsm.closeConnection()
        if (LOG.isDebugEnabled) {
            LOG.debug("IDSCP2 connection {} closed", id)
        }
    }

    /**
     * Send data to the peer IDSCP2 connector
     */
    override fun send(msg: ByteArray) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Sending data via connection {}...", id)
        }
        fsm.send(msg)
    }

    override fun onMessage(msg: ByteArray) {
        // When unlock is called, although not synchronized, this will eventually stop blocking.
        messageLatch.await()
        if (LOG.isTraceEnabled) {
            LOG.trace("Received new IDSCP Message")
        }
        messageListeners.forEach(Consumer { l: Idscp2MessageListener -> l.onMessage(this, msg) })
    }

    override fun onError(t: Throwable) {
        connectionListeners.forEach(Consumer { idscp2ConnectionListener: Idscp2ConnectionListener -> idscp2ConnectionListener.onError(t) })
    }

    override fun onClose() {
        LOG.debug("Connection with id {} is closing, notify listeners...", id)
        connectionListeners.forEach(Consumer { l: Idscp2ConnectionListener -> l.onClose() })
    }

    /**
     * Check if the idscp connection is currently established
     *
     * @return Connection established state
     */
    override val isConnected: Boolean
        get() = fsm.isConnected

    override fun addConnectionListener(listener: Idscp2ConnectionListener) {
        connectionListeners.add(listener)
    }

    override fun removeConnectionListener(listener: Idscp2ConnectionListener): Boolean {
        return connectionListeners.remove(listener)
    }

    override fun addMessageListener(listener: Idscp2MessageListener) {
        messageListeners.add(listener)
    }

    override fun removeMessageListener(listener: Idscp2MessageListener): Boolean {
        return messageListeners.remove(listener)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ConnectionImpl::class.java)
    }

    init {
        secureChannel.setFsm(fsm)
        if (LOG.isDebugEnabled) {
            LOG.debug("A new IDSCP2 connection with id {} was created, starting handshake...", id)
        }
//        if (LOG.isTraceEnabled) {
//            LOG.trace("Stack Trace of Idscp2Connection {} constructor:\n"
//                    + Arrays.stream(Thread.currentThread().stackTrace)
//                    .skip(1).map { obj: StackTraceElement -> obj.toString() }.collect(Collectors.joining("\n")), id)
//        }
        // Schedule IDSCP handshake asynchronously
        CompletableFuture.runAsync { fsm.startIdscpHandshake() }
    }
}