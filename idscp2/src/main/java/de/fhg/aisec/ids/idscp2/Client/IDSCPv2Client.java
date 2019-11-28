package de.fhg.aisec.ids.idscp2.Client;

import de.fhg.aisec.ids.idscp2.Constants;
import de.fhg.aisec.ids.idscp2.DataAvailableListener;
import de.fhg.aisec.ids.idscp2.InputListener;
import de.fhg.aisec.ids.idscp2.InputListenerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

/**
 * An abstract client implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public abstract class IDSCPv2Client implements DataAvailableListener {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Client.class);

    protected ClientConfiguration clientConfiguration = null;
    protected Socket clientSocket = null;
    protected OutputStream out = null;
    protected InputStream in = null;
    protected InputListenerThread inputListenerThread = null;

    public IDSCPv2Client(){

    }

    public boolean connect(){
        if (clientSocket == null || clientSocket.isClosed()){
            LOG.error("Client socket is not available");
            return false;
        }

        try {
            clientSocket.connect(new InetSocketAddress(clientConfiguration.getHostname(),
                    clientConfiguration.getServerPort()));
            out = clientSocket.getOutputStream();
            in = clientSocket.getInputStream();

            //set clientSocket timeout to allow safeStop()
            clientSocket.setSoTimeout(5000);
            //start receiving listener
            inputListenerThread = new InputListenerThread(in);
            inputListenerThread.register(this);
            inputListenerThread.start();

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
                send(Constants.CLIENT_GOODBYE);
                inputListenerThread.safeStop();
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void send(byte[] data){
        if (!isConnected()){
            LOG.error("Client cannot send data because socket is not connected");
        } else {
            try {
                out.write(data);
                out.flush();
            } catch (IOException e){
                LOG.error("Client cannot send data");
                e.printStackTrace();
            }
        }
    }

    public void send(String data){
        send(data.getBytes());
    }

    /* this is called by a listener thread when data were received*/
    public void onMessage(byte[] data){
        if (Arrays.toString(data).equals(Constants.END_OF_STREAM)){
            //End of stream, connection is not available anymore
        }
        //toDo do something with the received data
        System.out.println("New data received: " + Arrays.toString(data));
    }

    public boolean isConnected(){
        return clientSocket != null && clientSocket.isConnected();
    }

}
