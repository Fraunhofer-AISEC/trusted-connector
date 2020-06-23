package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.TLSConstants;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.SecureChannelInitListener;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * A TLS Server that listens on a given port from the Idscp2Settings and create new
 * TLSServerThreads for incoming connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSServer implements Runnable, SecureServer {
    private static final Logger LOG = LoggerFactory.getLogger(TLSServer.class);

    private volatile boolean isRunning = false;
    private final ServerSocket serverSocket;
    private final SecureChannelInitListener secureChannelInitListener;
    private final CompletableFuture<ServerConnectionListener> serverListenerPromise;
    private final Thread serverThread;

    public TLSServer(Idscp2Settings serverSettings, SecureChannelInitListener secureChannelInitListener,
                     CompletableFuture<ServerConnectionListener> serverListenerPromise)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {
        this.secureChannelInitListener = secureChannelInitListener;
        this.serverListenerPromise = serverListenerPromise;

        /* init server for TCP/TLS communication */

        // Get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager,
        // which enables host verification and algorithm constraints
        LOG.debug("Creating trust manager for TLS server...");
        TrustManager[] myTrustManager = PreConfiguration.getX509ExtTrustManager(
                serverSettings.getTrustStorePath(),
                serverSettings.getTrustStorePassword()
        );

        // Get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager,
        // which enables connection specific key selection via key alias
        LOG.debug("Creating key manager for TLS server...");
        KeyManager[] myKeyManager = PreConfiguration.getX509ExtKeyManager(
                serverSettings.getKeyPassword(),
                serverSettings.getKeyStorePath(),
                serverSettings.getKeyStorePassword(),
                serverSettings.getCertificateAlias(),
                serverSettings.getKeyStoreKeyType()
        );

        LOG.debug("Setting TLS security attributes and creating TLS server socket...");
        // Create TLS context based on keyManager and trustManager
        SSLContext sslContext = SSLContext.getInstance(TLSConstants.TLS_INSTANCE);
        sslContext.init(myKeyManager, myTrustManager, null);
        SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();

        serverSocket = socketFactory.createServerSocket(serverSettings.getServerPort());
        // Set timeout for serverSocket.accept()
        serverSocket.setSoTimeout(5000);

        SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;

        // Set TLS constraints
        SSLParameters sslParameters = sslServerSocket.getSSLParameters();
        sslParameters.setUseCipherSuitesOrder(true); //server determines priority-order of algorithms in CipherSuite
        sslParameters.setNeedClientAuth(true); //client must authenticate
        sslParameters.setProtocols(TLSConstants.TLS_ENABLED_PROTOCOLS); //only TLSv1.3
        sslParameters.setCipherSuites(TLSConstants.TLS_ENABLED_CIPHERS); //only allow strong cipher suite
        sslServerSocket.setSSLParameters(sslParameters);

        LOG.debug("Starting TLS server...");
        serverThread = new Thread(this, "TLS Server Thread "
                + serverSettings.getHost() + ":" + serverSettings.getServerPort());
        serverThread.start();
    }

    @Override
    public void run() {
        final ServerSocket serverSocket;
        if ((serverSocket = this.serverSocket) == null || serverSocket.isClosed()) {
            LOG.error("ServerSocket is not available, server thread is stopping now.");
            return;
        }

        isRunning = true;
        LOG.debug("TLS server started, entering accept() loop...");
        while (isRunning) {
            try {
                final var sslSocket = (SSLSocket) serverSocket.accept();
                try {
                    // Start new server thread
                    LOG.debug("New TLS client has connected. Creating new server thread...");
                    final var serverThread = new TLSServerThread(sslSocket, secureChannelInitListener, serverListenerPromise);
                    sslSocket.addHandshakeCompletedListener(serverThread);
                    serverThread.start();
                } catch (Exception serverThreadException) {
                    LOG.error("Error whilst creating/starting TLSServerThread", serverThreadException);
                }
            } catch (SocketTimeoutException e) {
                //timeout on serverSocket blocking functions was reached
                //in this way we can catch safeStop() function, that makes isRunning false
                //without closing the serverSocket, so we can stop and restart the server
                //alternative: close serverSocket. But then we cannot reuse it
            } catch (SocketException e) {
                LOG.debug("Server socket has been closed.");
                isRunning = false;
            } catch (IOException e) {
                LOG.error("Error during TLS server socket accept, notifying error handlers...");
                secureChannelInitListener.onError(e);
                isRunning = false;
            }
        }

        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOG.warn("Could not close TLS server socket", e);
            }
        }
    }

    @Override
    public void safeStop() {
        LOG.debug("Stopping tls server");
        isRunning = false;
//        try {
//            serverSocket.close();
//        } catch (IOException e) {
//            LOG.warn("Trying to close server socket failed!", e);
//        }
        try {
            this.serverThread.join();
        } catch (InterruptedException e) {
            LOG.warn("InterruptedException whilst waiting for server stop", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

}
