package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

/**
 * A TLSServerThread that notifies an IDSCPv2Config when a secure channel was created and the TLS handshake is done
 * When new data are available the serverThread transfers them to the SecureChannelListener
 *
 * Developer API
 *
 * constructors:
 * TLSServerThread(IDSCPv2Settings, IDSCPv2Callback) initializes the TLS Socket
 *
 * Methods:
 * run()    runs the serverThread and starts listening for new data
 *
 * close()  disconnects the serverThread
 *
 *
 * handshakeCompleted()        create a secureChannel, including this serverThread and provides it to the IDSCPv2Config
 *
 * send(byte[] data)            send data to the client
 *
 * onMessage(int len, byte[] rawData) is called when new data are available. Transfer them to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServerThread extends Thread implements HandshakeCompletedListener, SecureChannelEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServerThread.class);

    private SSLSocket sslSocket;
    private volatile boolean running = true;
    private DataInputStream in;
    private DataOutputStream out;
    private SecureChannelListener listener = null;  // race conditions are avoided using CountDownLatch
    private IDSCPv2Callback configCallback;  //no race conditions
    private IdscpConnectionListener idscpServerCallback; //no race conditions
    private CountDownLatch listenerLatch = new CountDownLatch(1);


    TLSServerThread(SSLSocket sslSocket, IDSCPv2Callback configCallback, IdscpConnectionListener idscpServerCallback){
        this.sslSocket = sslSocket;
        this.configCallback = configCallback;
        this.idscpServerCallback = idscpServerCallback;

        try {
            //set timout for blocking read
            sslSocket.setSoTimeout(5000);
            in = new DataInputStream(sslSocket.getInputStream());
            out = new DataOutputStream(sslSocket.getOutputStream());
        } catch (IOException e){
            LOG.error(e.getMessage());
            running = false;
        }
    }

    @Override
    public void run(){
        //wait for new data while running
        byte[] buf;
        while (running){
            try {
                int len = in.readInt();
                buf = new byte[len];
                in.readFully(buf, 0, len);
                onMessage(buf);

            } catch (SocketTimeoutException e){
                //timeout catches safeStop() call and allows to send server_goodbye
                //alternative: close sslSocket and catch SocketException
                //continue
            } catch (EOFException e){
                onClose();
                running = false;
            } catch (IOException e){
                onError();
                running = false;
            }
        }
        closeSockets();
    }

    private void closeSockets(){
        try {
            out.close();
            in.close();
            sslSocket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public void send(byte[] data) {
        if (!isConnected()){
            LOG.error("Server cannot send data because socket is not connected");
            closeSockets();
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.trace("Send message: " + new String(data));
            } catch (IOException e){
                LOG.error("ServerThread cannot send data.");
                closeSockets();
            }
        }
    }

    private void onClose(){
        try {
            listenerLatch.await();
            listener.onClose();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void onError(){
        try {
            listenerLatch.await();
            listener.onError();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        safeStop();
    }

    @Override
    public void onMessage(byte[] data)  {
        try{
            listenerLatch.await();
            this.listener.onMessage(data);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private void safeStop(){
        running = false;
    }

    public boolean isConnected() {
        return (sslSocket != null && sslSocket.isConnected());
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        LOG.debug("TLS handshake was successful");
        SecureChannel secureChannel = new SecureChannel(this);
        this.listener = secureChannel;
        listenerLatch.countDown();
        configCallback.secureChannelListenHandler(secureChannel, idscpServerCallback);
    }
}
