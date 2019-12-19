package de.fhg.aisec.ids.idscp2;

import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;

/**
 * An interface for the IDSCPv2Initiator class, that implements a callback function notify the user, a new connection
 * was created
 *
 * Developer API
 *
 * Methods:
 * void onConnect(IDSCPv2Connection)       to notify the user, a new connection was created
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface IDSCPv2Initiator {
    void newConnectionHandler(IDSCPv2Connection connection);
    void errorHandler(String error);
    void connectionClosedHandler(String connectionId);
}
