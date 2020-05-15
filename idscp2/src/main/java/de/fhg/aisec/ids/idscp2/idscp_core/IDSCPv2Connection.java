package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * The IDSCPv2 Connection class holds connections between connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Connection implements IdscpMsgListener {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Connection.class);

    private FSM fsm;
    private String connectionId;
    private IDSCPv2Initiator user;
    private IdscpConnectionListener server;

    public IDSCPv2Connection(FSM fsm, String connectionId, IDSCPv2Initiator user, IdscpConnectionListener server){
        this.fsm = fsm;
        this.connectionId = connectionId;
        this.user = user;
        this.server = server;
    }

    /*
     * Close the idscp connection
     */
    public void close() {
        // unregister connection from the idscp server
        if (server != null) {
            server.connectionClosedHandler(connectionId);
        }
        fsm.terminate();
    }

    /*
     * Send data to the peer idscp connector
     */
    public void send(byte[] msg) {
        LOG.debug("Send data ");
        fsm.send(msg);
    }

    @Override
    public void onMessage(byte[] msg) {
        LOG.info("Received new IDSCP Message: " + Arrays.toString(msg));
    }

    @Override
    public void onClose() {
        LOG.debug("Connection with id {} has been closed, notify user", connectionId);
        user.connectionClosedHandler(connectionId);
        if (server != null) {
            server.connectionClosedHandler(connectionId);
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
}
