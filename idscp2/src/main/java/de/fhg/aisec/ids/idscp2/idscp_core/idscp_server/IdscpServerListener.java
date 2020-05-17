package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;

/**
 * An IDSCP Connection Listener interface that is implemented by the IDSCPv2 Server to notify the
 * server about lifetimes of IDSCP connections. The server caches all active connections.
 */
public interface IdscpServerListener {

    /*
     * Notify the server that a new connection was established
     */
    void onConnect(Idscp2Connection connection);

    /*
     * Notify the server that connection has been closed
     */
    void onClose(String connectionId);
}
