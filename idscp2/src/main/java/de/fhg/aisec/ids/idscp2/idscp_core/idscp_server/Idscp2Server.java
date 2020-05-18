package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An IDSCP2 Server that has the control about the underlying secure server and caches all active
 * connections that belong to the secure server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2Server {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2Server.class);

    private final SecureServer secureServer;
    private final ConcurrentHashMap<String, Idscp2Connection> connections = new ConcurrentHashMap<>();

    public Idscp2Server(SecureServer secureServer, CompletableFuture<Idscp2Connection> connectionPromise) {
        this.secureServer = secureServer;
        connectionPromise.thenAccept(connection -> connections.put(connection.getConnectionId(), connection));
    }

    /*
     * Terminate the IDSCP2 server, the secure server and close all connections
     */
    public void terminate(){
        LOG.info("Terminating idscp server {}", this.toString());
        secureServer.safeStop();
        terminateAllSessions();
    }

    /*
     * Close all open IDSCP2 connections of this server
     */
    public void terminateAllSessions(){
        for (Idscp2Connection c : connections.values()){
            c.close();
            LOG.debug("Idscp connection with id {} has been closed", c.getConnectionId());
            connections.remove(c.getConnectionId());
        }
    }

    /*
     * Check if the server is running
     */
    public boolean isRunning(){
        return secureServer.isRunning();
    }

    /*
     * Get a Idscp2Connection from the server cache by the connectionID
     *
     * return null if no connection was found with this ID
     */
    public Idscp2Connection getConnectionById(String connectionId){
        return connections.get(connectionId);
    }

    /*
     * return a list of all open IDSCP2 connections of this server
     */
    public Collection<Idscp2Connection> getAllConnections(){
        return Collections.unmodifiableCollection(connections.values());
    }

}
