package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection

import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.FastLatch
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2TimeoutException
import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2WouldBlockException
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
                           configuration: Idscp2Configuration): Idscp2Connection {
    private val fsm: FSM = FSM(
            this,
            secureChannel,
            configuration.dapsDriver,
            configuration.attestationConfig,
            configuration.ackTimeoutDelay,
            configuration.handshakeTimeoutDelay
    )
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

        when (val res = fsm.closeConnection()) {
            FSM.FsmResultCode.FSM_NOT_STARTED -> throw Idscp2Exception("Handshake not started: " + res.value)
            else -> if (LOG.isDebugEnabled) {
                LOG.debug("IDSCP2 connection {} closed", id)
            }
        }
    }

    /**
     * Send data to the peer IDSCP2 connector
     */

    override fun nonBlockingSend(msg: ByteArray) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Sending data via connection {}...", id)
        }

        when (val res = fsm.send(msg)) {
            FSM.FsmResultCode.OK -> return
            FSM.FsmResultCode.WOULD_BLOCK -> throw Idscp2WouldBlockException("Idscp2 connection still waiting for ack")
            FSM.FsmResultCode.IO_ERROR, FSM.FsmResultCode.FSM_LOCKED -> throw Idscp2Exception("Connection aborted: " + res.value)
            FSM.FsmResultCode.NOT_CONNECTED -> throw Idscp2Exception("Idscp2 connection temporarily not available")
            else -> throw Idscp2Exception("Idscp2 error occurred while sending data: " + res.value)
        }

    }

    override fun blockingSend(msg: ByteArray, timeout: Long, retryInterval: Long) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Sending data via connection {}...", id)
        }

        val start = System.currentTimeMillis()

        while (true) {
            val now = System.currentTimeMillis()
            if (now >= start + timeout) {
                throw Idscp2TimeoutException("Idscp2 connection temporarily not available")
            }

            when (val res = fsm.send(msg)) {
                FSM.FsmResultCode.OK -> return
                FSM.FsmResultCode.WOULD_BLOCK -> {
                    // wait and repeat, fsm currently in wait_for_ack state
                    if (retryInterval > 0)
                        Thread.sleep(retryInterval)
                    continue;
                }
                FSM.FsmResultCode.IO_ERROR, FSM.FsmResultCode.FSM_LOCKED -> throw Idscp2Exception("Connection aborted: " + res.value)
                FSM.FsmResultCode.NOT_CONNECTED -> throw Idscp2Exception("Idscp2 connection temporarily not available")
                else -> throw Idscp2Exception("Idscp2 error occurred while sending data: " + res.value)
            }
        }
    }

    override fun repeatRat() {
        if (LOG.isTraceEnabled) {
            LOG.trace("Repeat Rat for connection {}...", id)
        }

        // match result
        when(val res = fsm.repeatRat()) {
            FSM.FsmResultCode.OK -> return
            FSM.FsmResultCode.FSM_LOCKED, FSM.FsmResultCode.IO_ERROR -> throw Idscp2Exception("Connection aborted: " + res.value)
            FSM.FsmResultCode.RAT_ERROR -> throw Idscp2Exception("RAT action failed: " + res.value)
            else -> throw Idscp2Exception("Error occurred: " + res.value)
        }
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