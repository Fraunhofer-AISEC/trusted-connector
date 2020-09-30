package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client.TLSClient
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server.TLSServer
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.idscp_core.server.ServerConnectionListener
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
    override fun connect(connectionFactory: (SecureChannel, Idscp2Settings, DapsDriver) -> CC,
                         settings: Idscp2Settings,
                         dapsDriver: DapsDriver): CompletableFuture<CC> {
        val connectionFuture = CompletableFuture<CC>()
        try {
            val tlsClient = TLSClient(connectionFactory, settings, dapsDriver, connectionFuture)
            tlsClient.connect(settings.host, settings.serverPort)
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
    override fun listen(settings: Idscp2Settings, channelInitListener: SecureChannelInitListener<CC>,
                        serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>): SecureServer {
        return try {
            TLSServer(settings, channelInitListener, serverListenerPromise)
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