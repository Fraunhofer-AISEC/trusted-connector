package de.fhg.aisec.ids.idscp2.Server;

import de.fhg.aisec.ids.idscp2.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * A TLS server thread implementation for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSServerThread extends Thread implements ServerThread, HandshakeCompletedListener {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServerThread.class);

    private SSLSocket sslSocket;
    private volatile boolean running = true;
    private InputStream in;
    private OutputStream out;
    private boolean tlsHandshakeCompleted = false;

    TLSServerThread(SSLSocket sslSocket){
        this.sslSocket = sslSocket;
        try {
            //set timout for blocking read
            sslSocket.setSoTimeout(5000);
            in = sslSocket.getInputStream();
            out = sslSocket.getOutputStream();
        } catch (IOException e){
            LOG.error(e.getMessage());
            running = false;
        }
    }

    @Override
    public void run(){
        //wait for new data while running
        byte[] buf = new byte[2048];

        //wait until tls handshake is completed
        //toDo avoid busy waiting
        while(!tlsHandshakeCompleted){
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while (running){
            try {
                if (0 > in.read(buf, 0, buf.length - 1)) {
                    onMessage(Constants.END_OF_STREAM.getBytes());
                } else {
                    onMessage(buf);
                }
            } catch (SocketTimeoutException e){
                //timeout catches safeStop() call and allows to send server_goodbye
                //alternative: close sslSocket and catch SocketException
                //continue
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        try {
            send(Constants.SERVER_GOODBYE);
            out.close();
            in.close();
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(byte[] data) {
        if (!isConnected()){
            LOG.error("Server cannot send data because socket is not connected");
        } else {
            try {
                out.write(data);
                out.flush();
            } catch (IOException e){
                LOG.error("Server cannot send data");
                e.printStackTrace();
            }
        }
    }

    public void send(String data){
        send(data.getBytes());
    }

    public void onMessage(byte[] data) {
        if (Arrays.toString(data).equals(Constants.END_OF_STREAM) ||
                Arrays.toString(data).equals(Constants.CLIENT_GOODBYE)){
            //End of stream or client goodbye, connection is no longer available
            running = false; //terminate server
        } else {
            //toDo do something with the received data
            System.out.println("New data received: " + Arrays.toString(data));
        }
    }

    public void safeStop(){
        running = false;
    }

    @Override
    public boolean isConnected() {
        return (sslSocket != null && sslSocket.isConnected());
    }


    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        tlsHandshakeCompleted = true;
    }
}
