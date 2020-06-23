package de.fhg.aisec.ids.idscp2;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;

/**
 * An interface for the Idscp2EndpointListener class, that implements callback functions that notifies
 * the user about new connections, errors and closed connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface Idscp2EndpointListener {

    /*
     * Called when a new connection is established
     */
    void onConnection(Idscp2Connection connection);

    /*
     * Called when an error occurred in the underlying IDSCP2 protocol
     */
    void onError(Throwable t);
}
