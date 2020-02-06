package de.fhg.aisec.ids.idscp2.idscp_core.idscp_server;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class IDSCPv2Server implements IdscpConnectionListener{
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Server.class);

    private SecureServer secureServer; //no race conditions using CountDownLatch
    private CountDownLatch secureServerLatch = new CountDownLatch(1);
    private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();

    public IDSCPv2Server(){

    }

    public IDSCPv2Server(SecureServer secureServer){
        this.secureServer = secureServer;
        secureServerLatch.countDown();
    }

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

    public void terminateAllSessions(){
        for (IDSCPv2Connection c : connections.values()){
            c.close();
            LOG.debug("Idscp connection with id {} has been closed", c.getConnectionId());
            connections.remove(c.getConnectionId());
        }
    }

    public boolean isRunning(){
        try {
            secureServerLatch.await();
            return secureServer.isRunning();
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        return false;
    }

    public IDSCPv2Connection getConnectionById(String connectionId){
        return connections.get(connectionId);
    }

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

    public void setSecureServer(SecureServer secureServer) {
        this.secureServer = secureServer;
        secureServerLatch.countDown();
    }
}
