package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionImpl
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class Idscp2ClientInitiator {
    private lateinit var connectionFuture: CompletableFuture<Idscp2Connection>

    fun init(settings: Idscp2Settings) {
        val secureChannelDriver = NativeTLSDriver<Idscp2Connection>()
        val dapsDriver = DefaultDapsDriver(DefaultDapsDriverConfig.Builder()
                .setKeyStorePath(settings.keyStorePath)
                .setTrustStorePath(settings.trustStorePath)
                .setKeyStorePassword(settings.keyStorePassword)
                .setTrustStorePassword(settings.trustStorePassword)
                .setKeyAlias(settings.dapsKeyAlias)
                .setKeyPassword(settings.keyPassword)
                .setDapsUrl("https://daps.aisec.fraunhofer.de")
                .build())
        RatProverDriverRegistry.registerDriver(
                "Dummy", ::RatProverDummy, null)
        RatVerifierDriverRegistry.registerDriver(
                "Dummy", ::RatVerifierDummy, null)
        RatProverDriverRegistry.registerDriver(
                "TPM2d", ::TPM2dProver,
                TPM2dProverConfig.Builder().build()
        )
        RatVerifierDriverRegistry.registerDriver(
                "TPM2d", ::TPM2dVerifier,
                TPM2dVerifierConfig.Builder().build()
        )
        connectionFuture = secureChannelDriver.connect(::Idscp2ConnectionImpl, settings, dapsDriver)
        connectionFuture.thenAccept { connection: Idscp2Connection ->
            println("Client: New connection with id " + connection.id)
            connection.addConnectionListener(object : Idscp2ConnectionAdapter() {
                override fun onError(t: Throwable) {
                    LOG.error("Client connection error occurred", t)
                }

                override fun onClose() {
                    LOG.info("Client: Connection with id " + connection.id + " has been closed")
                }
            })
            connection.addMessageListener { c: Idscp2Connection, data: ByteArray ->
                println("Received ping message: " + String(data, StandardCharsets.UTF_8))
                CompletableFuture.runAsync {
                    println("Close Connection")
                    c.close()
                } // FSM error if run from the same thread
            }
            connection.unlockMessaging()
            println("Send PING ...")
            connection.send("PING".toByteArray(StandardCharsets.UTF_8))
        }.exceptionally { t: Throwable? ->
            LOG.error("Client endpoint error occurred", t)
            null
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientInitiator::class.java)
    }
}