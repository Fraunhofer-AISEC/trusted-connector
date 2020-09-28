package de.fhg.aisec.ids.idscp2.app_layer

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import java.util.concurrent.CompletableFuture

class AppLayerClientFactory(private val dapsDriver: DapsDriver,
                            private val secureChannelDriver: SecureChannelDriver<AppLayerConnection>) {
    /**
     * User API to create a IDSCP2 connection as a client
     */
    fun connect(settings: Idscp2Settings): CompletableFuture<AppLayerConnection> {
        val connectionFuture = CompletableFuture<AppLayerConnection>()
        secureChannelDriver.connect(::AppLayerConnection, settings, dapsDriver, connectionFuture)
        return connectionFuture
    }
}