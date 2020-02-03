package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IDSCPv2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * The IDSCPv2 Connection class
 *
 * User/Developer API:
 *
 * Methods:
 * void close()             to close an IDSCP connection
 * void send(IdscpMessage)  to send an idscp message to the other idscp endpoint
 * void onMessage()         to receive an asynchronous message from the other idscp endpoint
 * boolean isConnected()    to check if the idscp connection is still open
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

    public void close() {
        if (server != null) {
            server.connectionClosedHandler(connectionId);
        }
        fsm.terminate();
    }

    public void send(byte[] msg) {
        LOG.debug("Send idscp message");
        fsm.send(IdscpMessageFactory.getIdscpDataMessage(msg));
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

    public boolean isConnected() {
        return fsm.isConnected();
    }

    public String getConnectionId() {
        return connectionId;
    }
}
