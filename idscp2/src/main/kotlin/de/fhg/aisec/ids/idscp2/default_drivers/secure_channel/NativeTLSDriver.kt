package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel

import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.client.TLSClient
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.server.TLSServer
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureChannelDriver
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.SecureServer
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.ServerConnectionListener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.CompletableFuture

/**
 * An implementation of SecureChannelDriver interface on TLSv1.3
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class NativeTLSDriver<CC: Idscp2Connection> : SecureChannelDriver<CC> {
    /**
     * Performs an asynchronous client connect to a TLS server.
     */
    override fun connect(connectionFactory: (SecureChannel, Idscp2Configuration) -> CC,
                         configuration: Idscp2Configuration): CompletableFuture<CC> {
        val connectionFuture = CompletableFuture<CC>()
        try {
            val tlsClient = TLSClient(connectionFactory, configuration, connectionFuture)
            tlsClient.connect(configuration.host, configuration.serverPort)
        } catch (e: IOException) {
            connectionFuture.completeExceptionally(Idscp2Exception("Call to connect() has failed", e))
        } catch (e: NoSuchAlgorithmException) {
            connectionFuture.completeExceptionally(Idscp2Exception("Call to connect() has failed", e))
        } catch (e: KeyManagementException) {
            connectionFuture.completeExceptionally(Idscp2Exception("Call to connect() has failed", e))
        }
        return connectionFuture
    }

    /**
     * Creates and starts a new TLS Server instance.
     *
     * @return The SecureServer instance
     * @throws Idscp2Exception If any error occurred during server creation/start
     */
    override fun listen(configuration: Idscp2Configuration, channelInitListener: SecureChannelInitListener<CC>,
                        serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>): SecureServer {
        return try {
            TLSServer(configuration, channelInitListener, serverListenerPromise)
        } catch (e: IOException) {
            throw Idscp2Exception("Error while trying to to start SecureServer", e)
        } catch (e: NoSuchAlgorithmException) {
            throw Idscp2Exception("Error while trying to to start SecureServer", e)
        } catch (e: KeyManagementException) {
            throw Idscp2Exception("Error while trying to to start SecureServer", e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NativeTLSDriver::class.java)
    }
}