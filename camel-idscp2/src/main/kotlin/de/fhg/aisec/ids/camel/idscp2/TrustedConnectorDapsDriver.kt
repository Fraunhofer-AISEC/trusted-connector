package de.fhg.aisec.ids.camel.idscp2

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import java.nio.file.Paths

class TrustedConnectorDapsDriver(private val config: DefaultDapsDriverConfig) : DefaultDapsDriver(config) {
    override fun getToken(): ByteArray {
        Idscp2OsgiComponent.getTokenManager()?.acquireToken(
                config.dapsUrl,
                Paths.get(config.keyStorePath),
                config.keyStorePassword,
                config.keyAlias,
                Paths.get(config.trustStorePath))
        return Idscp2OsgiComponent.getSettings().dynamicAttributeToken.toByteArray()
    }
}