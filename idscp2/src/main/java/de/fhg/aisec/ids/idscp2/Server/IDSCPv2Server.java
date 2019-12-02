package de.fhg.aisec.ids.idscp2.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * An abstract server implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public abstract class IDSCPv2Server {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Server.class);

    protected ArrayList<ServerThread> servers = new ArrayList<>();

    protected ServerConfiguration serverConfiguration = null;
    protected volatile boolean isRunning = false;
    protected ServerSocket serverSocket = null;

    public IDSCPv2Server(ServerConfiguration serverConfiguration){
        this.serverConfiguration = serverConfiguration;
    }

    protected boolean start(){
        LOG.info("Starting server");
        return (isRunning = true);
    }

    public void stop(){
        LOG.info("Stopping server");
        isRunning = false;
        terminateRunningThreads();
    }

    public void close(){
        stop();
        if (serverSocket != null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.error("Could not close server socket");
                e.printStackTrace();
            }
        }
        terminateRunningThreads();
    }

    private void terminateRunningThreads(){
        for (ServerThread thread : servers){
            thread.safeStop();
        }
        servers.clear();
    }

    public boolean isRunning(){
        return isRunning;
    }
}
