package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TLSConstants;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TLSSessionVerificationHelper;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelEndpoint;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * A TLS Client that notifies an Idscp2ServerFactory when a secure channel was created and the
 * TLS handshake is done. The client is notified from an InputListenerThread when new data are
 * available and transfer it to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public class TLSClient implements HandshakeCompletedListener, DataAvailableListener, SecureChannelEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TLSClient.class);

    private final Socket clientSocket;
    private DataOutputStream out;
    private InputListenerThread inputListenerThread;
    private final CompletableFuture<SecureChannelListener> listenerPromise = new CompletableFuture<>();
    private final Idscp2Settings clientSettings;
    private final DapsDriver dapsDriver;
    private final CompletableFuture<Idscp2Connection> connectionFuture;

    public TLSClient(Idscp2Settings clientSettings,
                     DapsDriver dapsDriver,
                     CompletableFuture<Idscp2Connection> connectionFuture)
            throws IOException, KeyManagementException, NoSuchAlgorithmException {
        this.clientSettings = clientSettings;
        this.dapsDriver = dapsDriver;
        this.connectionFuture = connectionFuture;

        // init TLS Client

        // get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
        // hostVerification and algorithm constraints
        TrustManager[] myTrustManager = PreConfiguration.getX509ExtTrustManager(
                clientSettings.getTrustStorePath(),
                clientSettings.getTrustStorePassword()
        );

        // get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
        // connection specific key selection via key alias
        KeyManager[] myKeyManager = PreConfiguration.getX509ExtKeyManager(
                clientSettings.getKeyPassword(),
                clientSettings.getKeyStorePath(),
                clientSettings.getKeyStorePassword(),
                clientSettings.getCertificateAlias(),
                clientSettings.getKeyStoreKeyType()
        );

        SSLContext sslContext = SSLContext.getInstance(TLSConstants.TLS_INSTANCE);
        sslContext.init(myKeyManager, myTrustManager, null);

        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        // create server socket
        clientSocket = socketFactory.createSocket();

        SSLSocket sslSocket = (SSLSocket) clientSocket;

        // set TLS constraints
        SSLParameters sslParameters = sslSocket.getSSLParameters();
        sslParameters.setUseCipherSuitesOrder(false);  // use server priority order
        sslParameters.setNeedClientAuth(true);
        sslParameters.setProtocols(TLSConstants.TLS_ENABLED_PROTOCOLS);  // only TLSv1.3
        sslParameters.setCipherSuites(TLSConstants.TLS_ENABLED_CIPHERS);  // only allow strong cipher
//        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");  // is done in application layer
        sslSocket.setSSLParameters(sslParameters);
        LOG.debug("TLS Client was initialized successfully");
    }


    /**
     * Connect to TLS server and start TLS Handshake
     */
    public void connect(String hostname, int port) {
        SSLSocket sslSocket = (SSLSocket) clientSocket;
        if (sslSocket == null || sslSocket.isClosed()) {
            throw new Idscp2Exception("Client socket is not available");
        }

        try {
            sslSocket.connect(new InetSocketAddress(hostname, port));
            LOG.debug("Client is connected to server {}:{}", hostname, port);

            //set clientSocket timeout to allow safeStop()
            clientSocket.setSoTimeout(5000);

            out = new DataOutputStream(clientSocket.getOutputStream());

            //add inputListener but start it not before handshake is complete
            inputListenerThread = new InputListenerThread(clientSocket.getInputStream());
            inputListenerThread.register(this);

            sslSocket.addHandshakeCompletedListener(this);
            LOG.debug("Start TLS Handshake");
            sslSocket.startHandshake();
        } catch (SSLHandshakeException | SSLProtocolException e) {
            disconnect();
            throw new Idscp2Exception("TLS Handshake failed", e);
        } catch (IOException e) {
            disconnect();
            throw new Idscp2Exception("Connecting TLS client to server failed", e);
        }
    }

    private void disconnect() {
        LOG.debug("Disconnecting from TLS server...");
        //close listener
        if (inputListenerThread != null && inputListenerThread.isAlive()) {
            inputListenerThread.safeStop();
        }

        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                onError(e);
            }
        }
    }

    @Override
    public void onClose() {
        listenerPromise.thenAccept(SecureChannelListener::onClose);
    }

    @Override
    public void onError(Throwable t) {
        listenerPromise.thenAccept(listener -> listener.onError(t));
    }

    @Override
    public void close() {
        disconnect();
    }

    @Override
    public boolean send(byte[] data) {
        if (!isConnected()) {
            LOG.error("Client cannot send data because socket is not connected");
            return false;
        } else {
            try {
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                LOG.debug("Send message");
                return true;
            } catch (IOException e) {
                LOG.error("Client cannot send data");
                return false;
            }
        }
    }

    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
        //start receiving listener after TLS Handshake was successful
        if (LOG.isDebugEnabled()) {
            LOG.debug("TLS Handshake was successful");
        }

        // verify tls session on application layer: hostname verification, certificate validity
        try {
            TLSSessionVerificationHelper.verifyTlsSession(handshakeCompletedEvent.getSession());
            LOG.debug("TLS session is valid");
        } catch (SSLPeerUnverifiedException e) {
            disconnect();
            connectionFuture.completeExceptionally(
                    new Idscp2Exception("TLS session is not valid. Close TLS connection", e));
        }

        // Create secure channel, register secure channel as message listener and notify IDSCP2 Configuration.
        SecureChannel secureChannel = new SecureChannel(this);
        this.listenerPromise.complete(secureChannel);
        this.connectionFuture.complete(new Idscp2Connection(secureChannel, clientSettings, dapsDriver));
        inputListenerThread.start();
    }

    @Override
    public void onMessage(byte[] data) {
        listenerPromise.thenAccept(listener -> listener.onMessage(data));
    }
}
