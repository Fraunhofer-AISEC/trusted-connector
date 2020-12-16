package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.tlsv1_3.server

import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.tlsv1_3.TLSSessionVerificationHelper
import de.fhg.aisec.ids.idscp2.idscp_core.FastLatch
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureChannelEndpoint
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannelListener
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.ServerConnectionListener
import org.slf4j.LoggerFactory
import java.io.*
import java.net.SocketTimeoutException
import java.util.concurrent.CompletableFuture
import javax.net.ssl.HandshakeCompletedEvent
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket

/**
 * A TLSServerThread that notifies an IDSCP2Config when a secure channel was created and the
 * TLS handshake is done
 *
 *
 * When new data are available the serverThread transfers it to the SecureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TLSServerThread<CC : Idscp2Connection> internal constructor(
        private val sslSocket: SSLSocket,
        private val configCallback: SecureChannelInitListener<CC>,
        private val serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>) :
        Thread(), HandshakeCompletedListener, SecureChannelEndpoint, Closeable
{
    @Volatile
    private var running = true
    private val `in`: DataInputStream
    private val out: DataOutputStream
    private val channelListenerPromise = CompletableFuture<SecureChannelListener>()
    private val tlsVerificationLatch = FastLatch()
    override fun run() {
        // first run the tls handshake to enforce catching every error occurred during the handshake
        // before reading from buffer. Else if there exists any non-catched exception during handshake
        // the thread would wait forever in onError() until handshakeCompleteListener is called
        try {
            sslSocket.startHandshake()
            // Wait for TLS session verification
            tlsVerificationLatch.await()
        } catch (e: Exception) {
            LOG.warn("Exception occurred during SSL handshake. Quiting server thread...", e)
            onError(e)
            running = false
        }

        //wait for new data while running
        var buf: ByteArray
        while (running) {
            try {
                val len = `in`.readInt()
                buf = ByteArray(len)
                `in`.readFully(buf, 0, len)
                onMessage(buf)
            } catch (ignore: SocketTimeoutException) {
                // Timeout catches safeStop() call and allows to send server_goodbye
            } catch (e: EOFException) {
                onClose()
                running = false
            } catch (e: IOException) {
                onError(e)
                running = false
            }
        }
        closeSockets()
    }

    private fun closeSockets() {
        try {
            out.close()
            `in`.close()
            sslSocket.close()
        } catch (ignore: IOException) {
        }
    }

    override fun send(bytes: ByteArray): Boolean {
        return if (!isConnected) {
            LOG.warn("Server cannot send data because socket is not connected")
            closeSockets()
            false
        } else {
            try {
                out.writeInt(bytes.size)
                out.write(bytes)
                out.flush()
                true
            } catch (e: IOException) {
                LOG.warn("Server could not send data", e)
                closeSockets()
                false
            }
        }
    }

    private fun onClose() {
        channelListenerPromise.thenAccept { obj: SecureChannelListener -> obj.onClose() }
    }

    private fun onError(t: Throwable) {
        channelListenerPromise.thenAccept { secureChannelListener: SecureChannelListener -> secureChannelListener.onError(t) }
    }

    override fun close() {
        safeStop()
    }

    fun onMessage(bytes: ByteArray) {
        channelListenerPromise.thenAccept { listener: SecureChannelListener -> listener.onMessage(bytes) }
    }

    private fun safeStop() {
        running = false
    }

    override val isConnected: Boolean
        get() = sslSocket.isConnected

    override fun handshakeCompleted(handshakeCompletedEvent: HandshakeCompletedEvent) {
        if (LOG.isTraceEnabled) {
            LOG.trace("TLS Handshake was successful")
        }

        // verify tls session on application layer: hostname verification, certificate validity
        try {
            TLSSessionVerificationHelper.verifyTlsSession(handshakeCompletedEvent.session)
            if (LOG.isTraceEnabled) {
                LOG.trace("TLS session is valid")
            }
        } catch (e: SSLPeerUnverifiedException) {
            LOG.warn("TLS session is not valid. Close TLS connection", e)
            running = false // set running false before tlsVerificationLatch is decremented
            return
        } finally {
            tlsVerificationLatch.unlock()
        }

        //provide secure channel to IDSCP2 Config and register secure channel as listener
        val secureChannel = SecureChannel(this)
        channelListenerPromise.complete(secureChannel)
        configCallback.onSecureChannel(secureChannel, serverListenerPromise)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TLSServerThread::class.java)
    }

    init {
        // Set timeout for blocking read
        sslSocket.soTimeout = 5000
        `in` = DataInputStream(sslSocket.inputStream)
        out = DataOutputStream(sslSocket.outputStream)
    }
}