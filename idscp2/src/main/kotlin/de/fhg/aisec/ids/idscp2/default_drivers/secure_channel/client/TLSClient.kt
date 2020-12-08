package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.client

import de.fhg.aisec.ids.idscp2.default_drivers.keystores.PreConfiguration
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.NativeTlsConfiguration
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.TLSConstants
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.TLSSessionVerificationHelper
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureChannelEndpoint
import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener
import org.slf4j.LoggerFactory
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CompletableFuture
import javax.net.ssl.*

/**
 * A TLS Client that notifies an Idscp2ServerFactory when a secure channel was created and the
 * TLS handshake is done. The client is notified from an InputListenerThread when new data are
 * available and transfer it to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TLSClient<CC: Idscp2Connection>(
        private val connectionFactory: (SecureChannel, Idscp2Configuration) -> CC,
        private val clientConfiguration: Idscp2Configuration,
        nativeTlsConfiguration: NativeTlsConfiguration,
        private val connectionFuture: CompletableFuture<CC>
) : HandshakeCompletedListener, DataAvailableListener, SecureChannelEndpoint {
    private val clientSocket: Socket
    private var dataOutputStream: DataOutputStream? = null
    private lateinit var inputListenerThread: InputListenerThread
    private val listenerPromise = CompletableFuture<SecureChannelListener>()

    /**
     * Connect to TLS server and start TLS Handshake
     */
    fun connect(hostname: String?, port: Int) {
        val sslSocket = clientSocket as SSLSocket?
        if (sslSocket == null || sslSocket.isClosed) {
            throw Idscp2Exception("Client socket is not available")
        }
        try {
            sslSocket.connect(InetSocketAddress(hostname, port))
            LOG.debug("Client is connected to server {}:{}", hostname, port)

            //set clientSocket timeout to allow safeStop()
            clientSocket.soTimeout = 5000
            dataOutputStream = DataOutputStream(clientSocket.getOutputStream())

            // Add inputListener but start it not before handshake is complete
            inputListenerThread = InputListenerThread(clientSocket.getInputStream(), this)
            sslSocket.addHandshakeCompletedListener(this)
            LOG.debug("Start TLS Handshake")
            sslSocket.startHandshake()
        } catch (e: SSLHandshakeException) {
            // FIXME: Any such disconnect makes the server maintain a broken connection
            disconnect()
            throw Idscp2Exception("TLS Handshake failed", e)
        } catch (e: SSLProtocolException) {
            disconnect()
            throw Idscp2Exception("TLS Handshake failed", e)
        } catch (e: IOException) {
            // FIXME: Any such disconnect makes the server maintain a broken connection
            disconnect()
            throw Idscp2Exception("Connecting TLS client to server failed", e)
        }
    }

    private fun disconnect() {
        LOG.debug("Disconnecting from TLS server...")
        if (::inputListenerThread.isInitialized) {
            inputListenerThread.safeStop()
        }
        if (!clientSocket.isClosed) {
            try {
                clientSocket.close()
            } catch (e: IOException) {
                onError(e)
            }
        }
    }

    override fun onClose() {
        listenerPromise.thenAccept { obj: SecureChannelListener -> obj.onClose() }
    }

    override fun onError(e: Throwable) {
        listenerPromise.thenAccept { listener: SecureChannelListener -> listener.onError(e) }
    }

    override fun close() {
        disconnect()
    }

    override fun send(bytes: ByteArray): Boolean {
        return if (!isConnected) {
            LOG.error("Client cannot send data because socket is not connected")
            false
        } else {
            try {
                dataOutputStream?.let {
                    it.writeInt(bytes.size)
                    it.write(bytes)
                    it.flush()
                } ?: throw IOException("DataOutputStream not available")
                if (LOG.isTraceEnabled) {
                    LOG.trace("Sending message...")
                }
                true
            } catch (e: IOException) {
                LOG.error("Client cannot send data", e)
                false
            }
        }
    }

    override val isConnected: Boolean
        get() = clientSocket.isConnected

    override fun handshakeCompleted(handshakeCompletedEvent: HandshakeCompletedEvent) {
        //start receiving listener after TLS Handshake was successful
        if (LOG.isDebugEnabled) {
            LOG.debug("TLS Handshake was successful")
        }
        if (!connectionFuture.isCancelled) {
            // TODO: When server behavior fixed, disconnect and return here instantly
//            disconnect();
//            return;
        }

        // verify tls session on application layer: hostname verification, certificate validity
        try {
            TLSSessionVerificationHelper.verifyTlsSession(handshakeCompletedEvent.session)
            LOG.debug("TLS session is valid")
            // Create secure channel, register secure channel as message listener and notify IDSCP2 Configuration.
            val secureChannel = SecureChannel(this)
            // Try to complete, won't do anything if promise has been cancelled
            listenerPromise.complete(secureChannel)
            val connection = connectionFactory(secureChannel, clientConfiguration)
            inputListenerThread.start()
            // Try to complete, won't do anything if promise has been cancelled
            connectionFuture.complete(connection)
            if (connectionFuture.isCancelled) {
                connection.close()
            }
        } catch (e: SSLPeerUnverifiedException) {
            // FIXME: Any such disconnect makes the server maintain a broken connection
            disconnect()
            connectionFuture.completeExceptionally(
                    Idscp2Exception("TLS session is not valid. Close TLS connection", e))
        }
    }

    override fun onMessage(bytes: ByteArray) {
        listenerPromise.thenAccept { listener: SecureChannelListener -> listener.onMessage(bytes) }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TLSClient::class.java)
    }

    init {
        // init TLS Client

        // get array of TrustManagers, that contains only one instance of X509ExtendedTrustManager, which enables
        // hostVerification and algorithm constraints
        val myTrustManager = PreConfiguration.getX509ExtTrustManager(
                nativeTlsConfiguration.trustStorePath,
                nativeTlsConfiguration.trustStorePassword
        )

        // get array of KeyManagers, that contains only one instance of X509ExtendedKeyManager, which enables
        // connection specific key selection via key alias
        val myKeyManager = PreConfiguration.getX509ExtKeyManager(
                nativeTlsConfiguration.keyPassword,
                nativeTlsConfiguration.keyStorePath,
                nativeTlsConfiguration.keyStorePassword,
                nativeTlsConfiguration.certificateAlias,
                nativeTlsConfiguration.keyStoreKeyType
        )
        val sslContext = SSLContext.getInstance(TLSConstants.TLS_INSTANCE)
        sslContext.init(myKeyManager, myTrustManager, null)
        val socketFactory = sslContext.socketFactory

        // create server socket
        clientSocket = socketFactory.createSocket()
        val sslSocket = clientSocket as SSLSocket

        // set TLS constraints
        val sslParameters = sslSocket.sslParameters
        sslParameters.useCipherSuitesOrder = false // use server priority order
        sslParameters.needClientAuth = true
        sslParameters.protocols = TLSConstants.TLS_ENABLED_PROTOCOLS // only TLSv1.3
        sslParameters.cipherSuites = TLSConstants.TLS_ENABLED_CIPHERS // only allow strong cipher
        //        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");  // is done in application layer
        sslSocket.sslParameters = sslParameters
        LOG.debug("TLS Client was initialized successfully")
    }
}