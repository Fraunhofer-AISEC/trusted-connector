package de.fhg.aisec.ids.idscp2.app_layer

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ServerFactory
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.server.Idscp2Server
import de.fhg.aisec.ids.idscp2.idscp_core.server.ServerConnectionListener
import java.util.concurrent.CompletableFuture

class AppLayerServerFactory(
        endpointListener: Idscp2EndpointListener<AppLayerConnection>,
        settings: Idscp2Settings,
        dapsDriver: DapsDriver,
        private val secureChannelDriver: SecureChannelDriver<AppLayerConnection>
) {
    private val idscp2ServerFactory = Idscp2ServerFactory(
            ::AppLayerConnection, endpointListener, settings, dapsDriver, secureChannelDriver)

    /**
     * User API to create a new IDSCP2 Server that starts a Secure Server that listens to new
     * secure channels
     */
    @Throws(Idscp2Exception::class)
    fun listen(settings: Idscp2Settings): Idscp2Server<AppLayerConnection> {
        val serverListenerPromise = CompletableFuture<ServerConnectionListener<AppLayerConnection>>()
        val secureServer = secureChannelDriver.listen(settings, idscp2ServerFactory, serverListenerPromise)
        val server = Idscp2Server<AppLayerConnection>(secureServer)
        serverListenerPromise.complete(server)
        return server
    }
}