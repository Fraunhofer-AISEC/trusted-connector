package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;

/**
 * An IDSCP Connection Listener interface that is implemented by the IDSCPv2 Server to notify the
 * server about lifetimes of IDSCP connections. The server caches all active connections.
 */
public interface IdscpConnectionListener {

    /*
     * Notify the server that a new connection was established
     */
    void newConnectionHandler(IDSCPv2Connection connection);

    /*
     * Notify the server that connection has been closed
     */
    void connectionClosedHandler(String connectionId);
}
