package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TLSSessionVerificationHelper;
import de.fhg.aisec.ids.idscp2.idscp_core.FastLatch;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.SecureChannelInitListener;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

/**
 * A TLSServerThread that notifies an IDSCP2Config when a secure channel was created and the
 * TLS handshake is done
 * <p>
 * When new data are available the serverThread transfers it to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServerThread extends Thread implements HandshakeCompletedListener, SecureChannelEndpoint, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServerThread.class);

    private volatile boolean running = true;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final SSLSocket sslSocket;
    private final SecureChannelInitListener configCallback;
    private final CompletableFuture<ServerConnectionListener> serverListenerPromise;
    private final CompletableFuture<SecureChannelListener> channelListenerPromise = new CompletableFuture<>();
    private final FastLatch tlsVerificationLatch = new FastLatch();

    TLSServerThread(SSLSocket sslSocket, SecureChannelInitListener configCallback,
                    CompletableFuture<ServerConnectionListener> serverListenerPromise) throws IOException {
        this.sslSocket = sslSocket;
        this.configCallback = configCallback;
        this.serverListenerPromise = serverListenerPromise;
        // Set timeout for blocking read
        sslSocket.setSoTimeout(5000);
        in = new DataInputStream(sslSocket.getInputStream());
        out = new DataOutputStream(sslSocket.getOutputStream());
    }

    @Override
    public void run() {
        // first run the tls handshake to enforce catching every error occurred during the handshake
        // before reading from buffer. Else if there exists any non-catched exception during handshake
        // the thread would wait forever in onError() until handshakeCompleteListener is called
        try {
            sslSocket.startHandshake();
            // Wait for TLS session verification
            tlsVerificationLatch.await();
        } catch (IOException e) {
            LOG.warn("Exception occurred during SSL handshake. Quiting server thread...", e);
            running = false;
        }

        //wait for new data while running
        byte[] buf;
        while (running) {
            try {
                int len = in.readInt();
                buf = new byte[len];
                in.readFully(buf, 0, len);
                onMessage(buf);
            } catch (SocketTimeoutException ignore) {
                // Timeout catches safeStop() call and allows to send server_goodbye
            } catch (EOFException e) {
                onClose();
                running = false;
            } catch (IOException e) {
                onError();
                running = false;
            }
        }
        closeSockets();
    }

    private void closeSockets() {
        try {
            out.close();
            in.close();
            sslSocket.close();
        } catch (IOException ignore) {
        }
    }

    @Override
    public boolean send(byte[] data) {
        if (!isConnected()) {
            LOG.error("Server cannot send data because socket is not connected");
            closeSockets();
            return false;
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.trace("Send message: " + new String(data));
                return true;
            } catch (IOException e) {
                LOG.error("ServerThread cannot send data.");
                closeSockets();
                return false;
            }
        }
    }

    private void onClose() {
        channelListenerPromise.thenAccept(SecureChannelListener::onClose);
    }

    private void onError() {
        channelListenerPromise.thenAccept(SecureChannelListener::onError);
    }

    @Override
    public void close() {
        safeStop();
    }

    @Override
    public void onMessage(byte[] data) {
        channelListenerPromise.thenAccept(listener -> listener.onMessage(data));
    }

    private void safeStop() {
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
        } catch (SSLPeerUnverifiedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("TLS session is not valid. Close TLS connection", e);
            }
            running = false;  // set running false before tlsVerificationLatch is decremented
            return;
        } finally {
            tlsVerificationLatch.unlock();
        }

        //provide secure channel to IDSCP2 Config and register secure channel as listener
        SecureChannel secureChannel = new SecureChannel(this);
        this.channelListenerPromise.complete(secureChannel);
        configCallback.onSecureChannel(secureChannel, serverListenerPromise);
    }
}
