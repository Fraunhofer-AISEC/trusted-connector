package de.fhg.aisec.ids.idscp2.idscp_core.api

import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection

/**
 * An interface for the Idscp2EndpointListener class, that implements callback functions that notifies
 * the user about new connections, errors and closed connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface Idscp2EndpointListener<T: Idscp2Connection> {
    /*
     * Called when a new connection is established
     */
    fun onConnection(connection: T)

    /*
     * Called when an error occurred in the underlying IDSCP2 protocol
     */
    fun onError(t: Throwable)
}