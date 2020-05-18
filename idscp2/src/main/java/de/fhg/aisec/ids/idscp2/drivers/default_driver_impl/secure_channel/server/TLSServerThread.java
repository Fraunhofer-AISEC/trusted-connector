package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TLSSessionVerificationHelper;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * A TLSServerThread that notifies an IDSCP2Config when a secure channel was created and the
 * TLS handshake is done
 *
 * When new data are available the serverThread transfers it to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServerThread extends Thread implements HandshakeCompletedListener, SecureChannelEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServerThread.class);

    private final SSLSocket sslSocket;
    private volatile boolean running = true;
    private DataInputStream in;
    private DataOutputStream out;
    private SecureChannelListener listener = null;  // race conditions are avoided using CountDownLatch
    private final Idscp2Callback configCallback;
    private final CompletableFuture<Idscp2Connection> connectionPromise;
    private final CountDownLatch listenerLatch = new CountDownLatch(1);
    private final CountDownLatch tlsVerificationLatch = new CountDownLatch(1);


    TLSServerThread(SSLSocket sslSocket, Idscp2Callback configCallback,
                    CompletableFuture<Idscp2Connection> connectionPromise){
        this.sslSocket = sslSocket;
        this.configCallback = configCallback;
        this.connectionPromise = connectionPromise;

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
        // first run the tls handshake to enforce catching every error occurred during the handshake
        // before reading from buffer. Else if there exists any non-catched exception during handshake
        // the thread would wait forever in onError() until handshakeCompleteListener is called
        try {
            sslSocket.startHandshake();
            //wait for tls session verification
            while (true) {
                try {
                    tlsVerificationLatch.await();
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            LOG.warn("SSLHandshakeException occurred. Quit server session", e);
            running = false;
        }

        //wait for new data while running
        byte[] buf;
        while (running){
            try {
                int len = in.readInt();
                buf = new byte[len];
                in.readFully(buf, 0, len);
                onMessage(buf);

            } catch (SocketTimeoutException ignore) {
                //timeout catches safeStop() call and allows to send server_goodbye
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
        } catch (IOException ignore) {}
    }

    @Override
    public boolean send(byte[] data) {
        if (!isConnected()){
            LOG.error("Server cannot send data because socket is not connected");
            closeSockets();
            return false;
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.debug("Send message: " + new String(data));
                return true;
            } catch (IOException e){
                LOG.error("ServerThread cannot send data.");
                closeSockets();
                return false;
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("TLS Handshake was successful");
        }

        // verify tls session on application layer: hostname verification, certificate validity
        try {
            TLSSessionVerificationHelper.verifyTlsSession(handshakeCompletedEvent.getSession());
            LOG.debug("TLS session is valid");
            tlsVerificationLatch.countDown();
        } catch (SSLPeerUnverifiedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("TLS session is not valid. Close TLS connection", e);
            }
            running = false; //set running false before tlsVerificationLatch is decremented
            tlsVerificationLatch.countDown();
            return;
        }

        //provide secure channel to IDSCP2 Config and register secure channel as listener
        SecureChannel secureChannel = new SecureChannel(this);
        this.listener = secureChannel;
        listenerLatch.countDown();
        configCallback.secureChannelListenHandler(secureChannel, connectionPromise);
    }
}
