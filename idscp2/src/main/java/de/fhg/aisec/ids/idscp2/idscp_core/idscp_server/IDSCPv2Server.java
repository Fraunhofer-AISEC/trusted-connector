package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * An IDSCPv2 Server that has the control about the underlying secure server and caches all active
 * connections that belong to the secure server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Server implements IdscpConnectionListener{
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Server.class);

    private SecureServer secureServer; //no race conditions using CountDownLatch
    private CountDownLatch secureServerLatch = new CountDownLatch(1);
    private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();

    public IDSCPv2Server(){}

    public IDSCPv2Server(SecureServer secureServer){
        this.secureServer = secureServer;
        secureServerLatch.countDown();
    }

    /*
     * Terminate the IDSCPv2 server, the secure server and close all connections
     */
    public void terminate(){
        LOG.info("Terminating idscp server {}", this.toString());
        try {
            secureServerLatch.await();
            secureServer.safeStop();
            terminateAllSessions();
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Close all open IDSCPv2 connections of this server
     */
    public void terminateAllSessions(){
        for (IDSCPv2Connection c : connections.values()){
            c.close();
            LOG.debug("Idscp connection with id {} has been closed", c.getConnectionId());
            connections.remove(c.getConnectionId());
        }
    }

    /*
     * Check if the server is running
     */
    public boolean isRunning(){
        try {
            secureServerLatch.await();
            return secureServer.isRunning();
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /*
     * Get a IDSCPv2Connection from the server cache by the connectionID
     *
     * return null if no connection was found with this ID
     */
    public IDSCPv2Connection getConnectionById(String connectionId){
        return connections.get(connectionId);
    }

    /*
     * return a list of all open IDSCPv2 connections of this server
     */
    public List<IDSCPv2Connection> getAllConnections(){
        return new ArrayList<>(connections.values());
    }

    @Override
    public void newConnectionHandler(IDSCPv2Connection connection) {
        LOG.debug("Binding new idscpv2 connection to server {}", this.toString());
        connections.put(connection.getConnectionId(), connection);
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        LOG.debug("Removing connection with id {} from server storage", connectionId);
        connections.remove(connectionId);
    }

    /*
     * Set the corresponding secure server
     */
    public void setSecureServer(SecureServer secureServer) {
        this.secureServer = secureServer;
        secureServerLatch.countDown();
    }
}
