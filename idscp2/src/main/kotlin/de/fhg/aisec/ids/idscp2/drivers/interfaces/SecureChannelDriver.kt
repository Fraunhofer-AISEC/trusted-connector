package de.fhg.aisec.ids.idscp2.drivers.interfaces

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.SecureChannelInitListener
import de.fhg.aisec.ids.idscp2.idscp_core.server.ServerConnectionListener
import java.util.concurrent.CompletableFuture

/**
 * An interface for the IDSCP2 SecureChannelDriver class, that implements a connect() function
 * for IDSCP2 clients and a listen() function for IDSCP2 servers to connect the underlying layer
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureChannelDriver {
    /*
     * Asynchronous method to create a secure connection to a secure server
     */
    fun connect(settings: Idscp2Settings, dapsDriver: DapsDriver, connectionFuture: CompletableFuture<Idscp2Connection>)

    /*
     * Starting a secure server
     */
    fun listen(settings: Idscp2Settings, channelInitListener: SecureChannelInitListener,
               serverListenerPromise: CompletableFuture<ServerConnectionListener>): SecureServer
}