package de.fhg.aisec.ids.idscp2.Client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * An abstract client implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public abstract class IDSCPv2Client {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Client.class);

    protected ClientConfiguration clientConfiguration = null;
    protected Socket clientSocket = null;

    protected boolean connect(){
        if (clientSocket == null || clientSocket.isClosed()){
            LOG.error("Client socket is not available");
            return false;
        }

        try {
            clientSocket.connect(null, clientConfiguration.getServerPort());
        } catch (IOException e) {
            LOG.error("Connecting client to server failed");
            e.printStackTrace();
            disconnect();
            return false;
        }
        return true;
    }

    public void disconnect() {
        if (clientSocket != null && !clientSocket.isClosed()){
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected(){
        return clientSocket != null && clientSocket.isConnected();
    }

}
