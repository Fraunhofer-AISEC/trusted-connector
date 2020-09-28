package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionImpl
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ServerFactory
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

class Idscp2ServerInitiator : Idscp2EndpointListener<Idscp2Connection> {
    fun init(settings: Idscp2Settings) {
        val secureChannelDriver = NativeTLSDriver<Idscp2Connection>()
        val config = DefaultDapsDriverConfig.Builder()
                .setKeyStorePath(settings.keyStorePath)
                .setTrustStorePath(settings.trustStorePath)
                .setKeyStorePassword(settings.keyStorePassword)
                .setTrustStorePassword(settings.trustStorePassword)
                .setKeyAlias(settings.dapsKeyAlias)
                .setKeyPassword(settings.keyPassword)
                .setDapsUrl("https://daps.aisec.fraunhofer.de")
                .build()
        val dapsDriver: DapsDriver = DefaultDapsDriver(config)
        RatProverDriverRegistry.registerDriver(
                "Dummy", ::RatProverDummy, null)
        RatVerifierDriverRegistry.registerDriver(
                "Dummy", ::RatVerifierDummy, null)
        RatProverDriverRegistry.registerDriver(
                "TPM2d", ::TPM2dProver, TPM2dProverConfig.Builder().build()
        )
        RatVerifierDriverRegistry.registerDriver(
                "TPM2d", ::TPM2dVerifier, TPM2dVerifierConfig.Builder().build()
        )
        val serverConfig = Idscp2ServerFactory(
                ::Idscp2ConnectionImpl,
                this,
                settings,
                dapsDriver,
                secureChannelDriver
        )
        @Suppress("UNUSED_VARIABLE") val idscp2Server = serverConfig.listen(settings)

//        try {
//            Thread.sleep(40_000); //run server for 2 minutes
//        } catch (Exception ignored) {
//        } finally {
//            idscp2Server.closeConnection();
//        }
    }

    override fun onConnection(connection: Idscp2Connection) {
        println("Server: New connection with id " + connection.id)
        connection.addConnectionListener(object : Idscp2ConnectionAdapter() {
            override fun onError(t: Throwable) {
                LOG.error("Server connection error occurred", t)
            }

            override fun onClose() {
                LOG.info("Server: Connection with id " + connection.id + " has been closed")
            }
        })
        connection.addMessageListener { c: Idscp2Connection, data: ByteArray ->
            println("Received ping message: ${String(data, StandardCharsets.UTF_8)}".trimIndent())
            println("Sending PONG...")
            c.send("PONG".toByteArray(StandardCharsets.UTF_8))
        }
    }

    override fun onError(t: Throwable) {
        LOG.error("Server endpoint error occurred", t)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ServerInitiator::class.java)
    }
}