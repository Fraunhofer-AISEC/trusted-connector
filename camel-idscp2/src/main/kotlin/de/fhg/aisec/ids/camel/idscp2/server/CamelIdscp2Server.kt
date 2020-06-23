package de.fhg.aisec.ids.camel.idscp2.server

import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent
import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ServerFactory
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server
import java.util.*

class CamelIdscp2Server(serverSettings: Idscp2Settings) : Idscp2EndpointListener {
    private val server: Idscp2Server
    val listeners: MutableSet<Idscp2EndpointListener> = Collections.synchronizedSet(HashSet())

    init {
        val dapsDriverConfig = DefaultDapsDriverConfig.Builder()
                .setConnectorUUID(Idscp2OsgiComponent.getSettings().connectorConfig.connectorUUID)
                .setKeyStorePath(serverSettings.keyStorePath)
                .setTrustStorePath(serverSettings.trustStorePath)
                .setKeyStorePassword(serverSettings.keyStorePassword)
                .setTrustStorePassword(serverSettings.trustStorePassword)
                .setKeyAlias(serverSettings.dapsKeyAlias)
                .setDapsUrl(Idscp2OsgiComponent.getSettings().connectorConfig.dapsUrl)
                .build()
        val serverConfiguration = Idscp2ServerFactory(
                this,
                serverSettings,
                DefaultDapsDriver(dapsDriverConfig),
                NativeTLSDriver()
        )
        server = serverConfiguration.listen(serverSettings)
    }

    override fun onConnection(connection: Idscp2Connection) {
        listeners.forEach { it.onConnection(connection) }
    }

    override fun onError(t: Throwable) {
        listeners.forEach { it.onError(t) }
    }

    val allConnections: Set<Idscp2Connection> = server.allConnections

    fun terminate() {
        server.terminate()
    }
}