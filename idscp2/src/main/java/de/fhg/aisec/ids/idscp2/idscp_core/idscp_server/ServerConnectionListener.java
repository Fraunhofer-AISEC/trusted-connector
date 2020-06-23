package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;

public interface ServerConnectionListener {

    void onConnectionCreated(Idscp2Connection connection);

    void onConnectionClose(Idscp2Connection connection);
}
