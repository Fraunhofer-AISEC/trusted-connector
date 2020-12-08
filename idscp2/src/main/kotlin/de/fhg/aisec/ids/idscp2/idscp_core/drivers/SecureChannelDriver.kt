package de.fhg.aisec.ids.idscp2.idscp_core.drivers

import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server.ServerConnectionListener
import java.util.concurrent.CompletableFuture

/**
 * An interface for the IDSCP2 SecureChannelDriver class, that implements a connect() function
 * for IDSCP2 clients and a listen() function for IDSCP2 servers to connect the underlying layer
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureChannelDriver<CC : Idscp2Connection, SecureChannelConfiguration> {
    /*
     * Asynchronous method to create a secure connection to a secure server
     */
    fun connect(connectionFactory: (SecureChannel, Idscp2Configuration) -> CC,
                     configuration: Idscp2Configuration,
                     secureChannelConfig: SecureChannelConfiguration): CompletableFuture<CC>

    /*
     * Starting a secure server
     */
    fun listen(channelInitListener: SecureChannelInitListener<CC>,
               serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>,
                secureChannelConfig: SecureChannelConfiguration): SecureServer
}