package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpClose;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public IDSCPv2Connection(FSM fsm, String connectionId){
        this.fsm = fsm;
        this.connectionId = connectionId;
    }

    public void close() {
        fsm.terminate();
    }

    public void send(IdscpMessage msg) {
        LOG.debug("Send idscp message");
        fsm.send(msg);
    }

    @Override
    public void onMessage(IdscpMessage msg) {
        LOG.info("Received new IDSCP Message: " + msg.toString());
    }

    public boolean isConnected() {
        return fsm.isConnected();
    }

    public String getConnectionId() {
        return connectionId;
    }
}
