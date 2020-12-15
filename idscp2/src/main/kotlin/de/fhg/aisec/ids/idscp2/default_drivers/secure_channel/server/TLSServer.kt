package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.server

import de.fhg.aisec.ids.idscp2.default_drivers.keystores.PreConfiguration
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.NativeTlsConfiguration
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.TLSConstants
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.ServerConnectionListener
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureServer
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.CompletableFuture
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/**
 * A TLS Server that listens on a given port from the Idscp2Settings and create new
 * TLSServerThreads for incoming connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TLSServer<CC: Idscp2Connection>(nativeTlsConfiguration: NativeTlsConfiguration,
                                      private val secureChannelInitListener: SecureChannelInitListener<CC>,
                                      private val serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>):
        Runnable, SecureServer {
    @Volatile
    override var isRunning = false
        private set
    private val serverSocket: ServerSocket
    private val serverThread: Thread
    override fun run() {
        if (serverSocket.isClosed) {
            LOG.warn("ServerSocket has been closed, server thread is stopping now.")
            return
        }
        isRunning = true
        if (LOG.isTraceEnabled) {
            LOG.trace("TLS server started, entering accept() loop...")
        }
        while (isRunning) {
            try {
                /*
                 * accept for new (incoming) ssl sockets. A timeout exception is thrown
                 * after 5 seconds of inactivity to allow a clean stop via the safeStop() method
                 */
                val sslSocket = serverSocket.accept() as SSLSocket
                try {
                    // Start new server thread
                    if (LOG.isTraceEnabled) {
                        LOG.trace("New TLS client has connected. Creating new server thread...")
                    }
                    val serverThread = TLSServerThread(sslSocket, secureChannelInitListener, serverListenerPromise)
                    sslSocket.addHandshakeCompletedListener(serverThread)
                    serverThread.start()
                } catch (serverThreadException: Exception) {
                    LOG.warn("Error whilst creating/starting TLSServerThread", serverThreadException)
                }
            } catch (e: SocketTimeoutException) {
                //timeout on serverSocket blocking functions was reached
                //in this way we can catch safeStop() function, that makes isRunning false
                //without closing the serverSocket, so we can stop and restart the server
                //alternative: close serverSocket. But then we cannot reuse it
            } catch (e: SocketException) {
                if (LOG.isTraceEnabled) {
                    LOG.trace("Server socket has been closed.")
                }
                isRunning = false
            } catch (e: IOException) {
                LOG.warn("Error during TLS server socket accept, notifying error handlers...")
                secureChannelInitListener.onError(e)
                isRunning = false
            }
        }
        if (!serverSocket.isClosed) {
            try {
                serverSocket.close()
            } catch (e: IOException) {
                LOG.warn("Could not close TLS server socket", e)
            }
        }
    }

    override fun safeStop() {
        if (LOG.isTraceEnabled) {
            LOG.trace("Stopping tls server")
        }
        /*
         * Set the volatile running variable to false. The serverSocket will receive
         * a socket timeout after at least 5 seconds while it is blocked in accept()
         * and will then check again if isRunning is still true
         */
        isRunning = false

//        try {
//            serverSocket.close();
//        } catch (IOException e) {
//            LOG.warn("Trying to close server socket failed!", e);
//        }

        try {
            serverThread.join()
        } catch (e: InterruptedException) {
            LOG.warn("InterruptedException whilst waiting for server stop", e)
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TLSServer::class.java)
    }

    init {

        /* init server for TCP/TLS communication */

        // Get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager,
        // which enables host verification and algorithm constraints
        if (LOG.isTraceEnabled) {
            LOG.trace("Creating trust manager for TLS server...")
        }
        val myTrustManager = PreConfiguration.getX509ExtTrustManager(
                nativeTlsConfiguration.trustStorePath,
                nativeTlsConfiguration.trustStorePassword
        )

        // Get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager,
        // which enables connection specific key selection via key alias
        if (LOG.isTraceEnabled) {
            LOG.trace("Creating key manager for TLS server...")
        }
        val myKeyManager = PreConfiguration.getX509ExtKeyManager(
                nativeTlsConfiguration.keyPassword,
                nativeTlsConfiguration.keyStorePath,
                nativeTlsConfiguration.keyStorePassword,
                nativeTlsConfiguration.certificateAlias,
                nativeTlsConfiguration.keyStoreKeyType
        )

        if (LOG.isTraceEnabled) {
            LOG.trace("Setting TLS security attributes and creating TLS server socket...")
        }
        // Create TLS context based on keyManager and trustManager
        val sslContext = SSLContext.getInstance(TLSConstants.TLS_INSTANCE)
        sslContext.init(myKeyManager, myTrustManager, null)
        val socketFactory = sslContext.serverSocketFactory
        serverSocket = socketFactory.createServerSocket(nativeTlsConfiguration.serverPort)
        // Set timeout for serverSocket.accept()
        serverSocket.soTimeout = 5000
        val sslServerSocket = serverSocket as SSLServerSocket

        // Set TLS constraints
        val sslParameters = sslServerSocket.sslParameters
        sslParameters.useCipherSuitesOrder = true //server determines priority-order of algorithms in CipherSuite
        sslParameters.needClientAuth = true //client must authenticate
        sslParameters.protocols = TLSConstants.TLS_ENABLED_PROTOCOLS //only TLSv1.3
        sslParameters.cipherSuites = TLSConstants.TLS_ENABLED_CIPHERS //only allow strong cipher suite
        sslServerSocket.sslParameters = sslParameters

        if (LOG.isTraceEnabled) {
            LOG.trace("Starting TLS server...")
        }
        serverThread = Thread(this, "TLS Server Thread "
                + nativeTlsConfiguration.host + ":" + nativeTlsConfiguration.serverPort)
        serverThread.start()
    }
}