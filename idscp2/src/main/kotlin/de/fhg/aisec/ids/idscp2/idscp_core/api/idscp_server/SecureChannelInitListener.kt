package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_server

import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import java.util.concurrent.CompletableFuture

/**
 * An callback interface that implements callback functions that notify about new
 * SecureChannels
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureChannelInitListener<CC: Idscp2Connection> {
    /**
     * Notify the server about new secureChannel
     */
    fun onSecureChannel(
            secureChannel: SecureChannel,
            serverListenerPromise: CompletableFuture<ServerConnectionListener<CC>>
    )

    fun onError(t: Throwable)
}