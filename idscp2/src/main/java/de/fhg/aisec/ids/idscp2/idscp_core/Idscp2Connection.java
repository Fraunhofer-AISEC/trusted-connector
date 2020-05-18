package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private final Set<Idscp2ConnectionListener> connectionListeners = new HashSet<>();
    private final Set<Idscp2MessageListener> genericMessageListeners = new HashSet<>();
    private final Map<String, Set<Idscp2MessageListener>> messageListeners = new HashMap<>();

    public Idscp2Connection(FSM fsm, String connectionId) {
        this.fsm = fsm;
        this.connectionId = connectionId;
    }

    /*
     * Close the idscp connection
     */
    public void close() {
        // unregister connection from the idscp server
        synchronized (connectionListeners) {
            connectionListeners.forEach(l -> l.onClose(connectionId));
        }
        fsm.terminate();
    }

    /*
     * Send data to the peer idscp connector
     */
    public void send(String type, byte[] msg) {
        LOG.debug("Send data of type \"" + type + "\" ");
        fsm.send(type, msg);
    }

    public void onMessage(String type, byte[] msg) {
        LOG.debug("Received new IDSCP Message: " + Arrays.toString(msg));
        synchronized (genericMessageListeners) {
            genericMessageListeners.forEach(l -> l.onMessage(type, msg));
        }
        Set<Idscp2MessageListener> listeners = messageListeners.get(type);
        if (listeners != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (listeners) {
                listeners.forEach(l -> l.onMessage(type, msg));
            }
        }
    }

    public void onClose() {
        LOG.debug("Connection with id {} has been closed, notify user", connectionId);
        synchronized (connectionListeners) {
            connectionListeners.forEach(l -> l.onClose(connectionId));
        }
    }

    /*
     * Check if the idscp connection is currently established
     */
    public boolean isConnected() {
        return fsm.isConnected();
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void addConnectionListener(Idscp2ConnectionListener listener) {
        synchronized (connectionListeners) {
            connectionListeners.add(listener);
        }
    }

    public boolean removeConnectionListener(Idscp2ConnectionListener listener) {
        synchronized (connectionListeners) {
            return connectionListeners.remove(listener);
        }
    }

    public void addGenericMessageListener(Idscp2MessageListener listener) {
        synchronized (genericMessageListeners) {
            genericMessageListeners.add(listener);
        }
    }

    public boolean removeGenericMessageListener(Idscp2MessageListener listener) {
        synchronized (genericMessageListeners) {
            return genericMessageListeners.remove(listener);
        }
    }

    public void addMessageListener(@Nullable String type, Idscp2MessageListener listener) {
        synchronized (messageListeners) {
            Set<Idscp2MessageListener> messageTypeListeners
                = messageListeners.computeIfAbsent(type, k -> new HashSet<>());
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (messageTypeListeners) {
                messageTypeListeners.add(listener);
            }
            messageListeners.put(type, messageTypeListeners);
        }
    }

    public boolean removeMessageListener(@Nullable String type, Idscp2MessageListener listener) {
        synchronized (messageListeners) {
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
}
