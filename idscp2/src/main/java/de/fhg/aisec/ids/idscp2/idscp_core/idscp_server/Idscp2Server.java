package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An IDSCP2 Server that has the control about the underlying secure server and caches all active
 * connections that belong to the secure server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2Server implements ServerConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2Server.class);

    private final SecureServer secureServer;
    private final Set<Idscp2Connection> connections = Collections.synchronizedSet(new HashSet<>());

    public Idscp2Server(SecureServer secureServer) {
        this.secureServer = secureServer;
    }

    /*
     * Terminate the IDSCP2 server, the secure server and close all connections
     */
    public void terminate() {
        LOG.info("Terminating IDSCP2 server {}", this.toString());
        terminateAllSessions();
        secureServer.safeStop();
    }

    /*
     * Close all open IDSCP2 connections of this server
     */
    public void terminateAllSessions() {
        for (Idscp2Connection connection : connections) {
            connection.close();
            LOG.debug("Idscp connection with id {} has been closed", connection.getId());
            connections.remove(connection);
        }
    }

    public void onConnectionCreated(Idscp2Connection connection) {
        connections.add(connection);
    }

    public void onConnectionClose(Idscp2Connection connection) {
        connections.remove(connection);
    }

    /*
     * Check if the server is running
     */
    public boolean isRunning() {
        return secureServer.isRunning();
    }

    /*
     * return a list of all open IDSCP2 connections of this server
     */
    public Set<Idscp2Connection> getAllConnections() {
        return Collections.unmodifiableSet(connections);
    }

}
