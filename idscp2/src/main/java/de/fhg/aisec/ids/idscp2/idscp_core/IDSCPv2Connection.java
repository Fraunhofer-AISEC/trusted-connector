package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpClose;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;

/**
 * The IDSCPv2 Connection class is the entry point for the IDSCPv2 protocol. It runs a peer (client or server) and
 * establishes a secure state by building a secure transfer channel, verifies the dynamicAttributeToken and RAT
 *
 * Developer API:
 *
 * Constructors:
 * IDSCPv2Connection(SecureChannel sc, String connectionID)
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

    private SecureChannel secureChannel;
    private String connectionId;

    public IDSCPv2Connection(SecureChannel secureChannel, String connectionId){
        this.secureChannel = secureChannel;
        this.connectionId = connectionId;
        secureChannel.setEndpointConnectionId(connectionId);
    }

    public void close() {
        IdscpMessage msg = IdscpMessage.newBuilder()
                .setType(IdscpMessage.Type.IDSCP_CLOSE)
                .setIdscpClose(IdscpClose.newBuilder().build()
                ).build();
        send(msg);
        secureChannel.close();
    }

    public void send(IdscpMessage msg) {
        secureChannel.send(msg);
    }

    @Override
    public void onMessage(IdscpMessage msg) {
        System.out.println("Received new IDSCP Message: " + msg.toString());
    }

    public boolean isConnected() {
        return secureChannel.isConnected();
    }

    public String getConnectionId() {
        return connectionId;
    }
}
