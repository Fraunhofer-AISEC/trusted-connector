package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureServer
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import org.slf4j.LoggerFactory
import java.util.*

/**
 * An IDSCP2 Server that has the control about the underlying secure server and caches all active
 * connections that belong to the secure server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2Server<CC: Idscp2Connection>(private val secureServer: SecureServer): ServerConnectionListener<CC> {
    private val connections = Collections.synchronizedSet(HashSet<CC>())

    /**
     * Terminate the IDSCP2 server, the secure server and close all connections
     */
    fun terminate() {
        if (LOG.isInfoEnabled) {
            LOG.info("Terminating IDSCP2 server {}", this.toString())
        }

        for (connection in connections) {
            connection.close()
            if (LOG.isDebugEnabled) {
                LOG.debug("Idscp connection with id {} has been closed", connection.id)
            }
            connections.remove(connection)
        }
        secureServer.safeStop()
    }

    override fun onConnectionCreated(connection: CC) {
        connections.add(connection)
    }

    override fun onConnectionClose(connection: CC) {
        connections.remove(connection)
    }

    /**
     * If the server is running
     */
    val isRunning: Boolean
        get() = secureServer.isRunning

    /**
     * List of all open IDSCP2 connections of this server
     */
    val allConnections: Set<CC>
        get() = Collections.unmodifiableSet(connections)

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2Server::class.java)
    }
}