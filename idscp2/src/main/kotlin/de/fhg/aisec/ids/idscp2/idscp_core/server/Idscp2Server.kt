package de.fhg.aisec.ids.idscp2.idscp_core.server

import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import org.slf4j.LoggerFactory
import java.util.*

/**
 * An IDSCP2 Server that has the control about the underlying secure server and caches all active
 * connections that belong to the secure server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2Server(private val secureServer: SecureServer?) : ServerConnectionListener {
    private val connections = Collections.synchronizedSet(HashSet<Idscp2Connection>())

    /*
     * Terminate the IDSCP2 server, the secure server and close all connections
     */
    fun terminate() {
        LOG.info("Terminating IDSCP2 server {}", this.toString())
        terminateAllSessions()
        secureServer!!.safeStop()
    }

    /*
     * Close all open IDSCP2 connections of this server
     */
    fun terminateAllSessions() {
        for (connection in connections) {
            connection.close()
            LOG.debug("Idscp connection with id {} has been closed", connection.id)
            connections.remove(connection)
        }
    }

    override fun onConnectionCreated(connection: Idscp2Connection) {
        connections.add(connection)
    }

    override fun onConnectionClose(connection: Idscp2Connection?) {
        connections.remove(connection)
    }

    /*
     * Check if the server is running
     */
    val isRunning: Boolean
        get() = secureServer!!.isRunning

    /*
     * return a list of all open IDSCP2 connections of this server
     */
    val allConnections: Set<Idscp2Connection>
        get() = Collections.unmodifiableSet(connections)

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2Server::class.java)
    }
}