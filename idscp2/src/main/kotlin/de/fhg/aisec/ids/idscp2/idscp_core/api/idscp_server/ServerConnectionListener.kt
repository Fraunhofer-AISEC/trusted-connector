package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server

import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection

/**
 * A ServerConnectionListener interface for the Idscp2Server to get notification about
 * Idscp2Connection lifetimes in an isolated way
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface ServerConnectionListener<CC: Idscp2Connection> {
    fun onConnectionCreated(connection: CC)
    fun onConnectionClose(connection: CC)
}