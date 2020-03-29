package de.fhg.aisec.ids.idscp2;

import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;

/**
 * An interface for the IDSCPv2Initiator class, that implements callback functions that notifies
 * the user about new connections, errors and closed connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface IDSCPv2Initiator {

    /*
     * Called when a new connection is established
     */
    void newConnectionHandler(IDSCPv2Connection connection);

    /*
     * Called when an error occurred in the underlying IDSCPv2 protocol
     */
    void errorHandler(String error);

    /*
     * Called when a connection with the given connectionID was closed
     */
    void connectionClosedHandler(String connectionId);
}
