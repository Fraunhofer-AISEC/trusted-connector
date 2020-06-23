package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The IDSCP2 Connection class holds connections between connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
public class Idscp2Connection {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2Connection.class);

    private final FSM fsm;
    private final String connectionId;
    private final Set<Idscp2ConnectionListener> connectionListeners = Collections.synchronizedSet(new HashSet<>());
    private final Set<Idscp2MessageListener> genericMessageListeners = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Set<Idscp2MessageListener>> messageListeners = new HashMap<>();
    private final FastLatch messageLatch = new FastLatch();

    public Idscp2Connection(SecureChannel secureChannel, Idscp2Settings settings, DapsDriver dapsDriver) {
        this.connectionId = UUID.randomUUID().toString();
        fsm = new FSM(
                this,
                secureChannel,
                dapsDriver,
                settings.getSupportedAttestation().getRatMechanisms(),
                settings.getExpectedAttestation().getRatMechanisms(),
                settings.getRatTimeoutDelay());
        secureChannel.setFsm(fsm);
        if (LOG.isDebugEnabled()) {
            LOG.debug("A new IDSCP2 connection with id {} was created, starting handshake...", connectionId);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stack Trace of Idscp2Connection {} constructor:\n"
                    + Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(1).map(Object::toString).collect(Collectors.joining("\n")), connectionId);
        }
        // Schedule IDSCP handshake asynchronously
        CompletableFuture.runAsync(fsm::startIdscpHandshake);
    }

    public void unlockMessaging() {
        messageLatch.unlock();
    }

    /**
     * Close the idscp connection
     */
    public void close() {
        LOG.debug("Closing connection {}...", connectionId);
        fsm.closeConnection();
        LOG.debug("IDSCP2 connection {} closed", connectionId);
    }

    /**
     * Send data to the peer IDSCP2 connector
     */
    public void send(String type, byte[] msg) {
        LOG.debug("Send data of type \"" + type + "\" via connection {}", connectionId);
        fsm.send(type, msg);
    }

    public void onMessage(String type, byte[] msg) {
        // When unlock is called, although not synchronized, this will eventually stop blocking.
        messageLatch.await();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received new IDSCP Message");
        }
        genericMessageListeners.forEach(l -> l.onMessage(this, type, msg));
        Set<Idscp2MessageListener> listeners = messageListeners.get(type);
        if (listeners != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (listeners) {
                listeners.forEach(l -> l.onMessage(this, type, msg));
            }
        }
    }

    public void onError(Throwable t) {
        connectionListeners.forEach(idscp2ConnectionListener -> idscp2ConnectionListener.onError(t));
    }

    public void onClose() {
        LOG.debug("Connection with id {} is closing, notify listeners...", connectionId);
        connectionListeners.forEach(l -> l.onClose(this));
    }

    /**
     * Check if the idscp connection is currently established
     *
     * @return Connection established state
     */
    @SuppressWarnings("unused")
    public boolean isConnected() {
        return fsm.isConnected();
    }

    public String getId() {
        return connectionId;
    }

    public void addConnectionListener(Idscp2ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @SuppressWarnings("unused")
    public boolean removeConnectionListener(Idscp2ConnectionListener listener) {
        return connectionListeners.remove(listener);
    }

    public void addGenericMessageListener(Idscp2MessageListener listener) {
        genericMessageListeners.add(listener);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeGenericMessageListener(Idscp2MessageListener listener) {
        return genericMessageListeners.remove(listener);
    }

    public void addMessageListener(@Nullable String type, Idscp2MessageListener listener) {
        Set<Idscp2MessageListener> messageTypeListeners
                = messageListeners.computeIfAbsent(type, k -> new HashSet<>());
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageTypeListeners) {
            messageTypeListeners.add(listener);
        }
        messageListeners.put(type, messageTypeListeners);
    }

    @SuppressWarnings("unused")
    public boolean removeMessageListener(@Nullable String type, Idscp2MessageListener listener) {
        Set<Idscp2MessageListener> messageTypeListeners = messageListeners.get(type);
        if (messageTypeListeners == null) {
            return false;
        }
        final boolean ret;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageTypeListeners) {
            ret = messageTypeListeners.remove(listener);
        }
        if (messageTypeListeners.isEmpty()) {
            messageListeners.remove(type);
        }
        return ret;
    }
}
