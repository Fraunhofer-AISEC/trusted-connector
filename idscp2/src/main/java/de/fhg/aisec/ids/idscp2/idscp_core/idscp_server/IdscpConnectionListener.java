package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;

public interface IdscpConnectionListener {
    void newConnectionHandler(IDSCPv2Connection connection);
    void connectionClosedHandler(String connectionId);
}
