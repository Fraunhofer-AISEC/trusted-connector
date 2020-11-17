package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.NullDaps
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionImpl
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class CommandlineTunnelClient {
    private lateinit var connectionFuture: CompletableFuture<Idscp2Connection>

    fun init(configuration: Idscp2Configuration) {
        LOG.info("Setting up IDSCP connection")

        // create secure channel driver
        val secureChannelDriver = NativeTLSDriver<Idscp2Connection>()

        // create daps driver
        val dapsDriver = NullDaps()

        // register rat drivers
        RatProverDriverRegistry.registerDriver(
                RatProverDummy.RAT_PROVER_DUMMY_ID, ::RatProverDummy, null)

        RatVerifierDriverRegistry.registerDriver(
                RatVerifierDummy.RAT_VERIFIER_DUMMY_ID, ::RatVerifierDummy, null)

        // connect to idscp2 server
        LOG.info("connecting to {}:{}", configuration.host, configuration.serverPort)
        connectionFuture = secureChannelDriver.connect(::Idscp2ConnectionImpl, configuration, dapsDriver)

        connectionFuture.thenAccept { connection: Idscp2Connection ->
            println("Client: New connection with id " + connection.id)
            var runningUserJob = true

            connection.addConnectionListener(object : Idscp2ConnectionAdapter() {
                override fun onError(t: Throwable) {
                    LOG.error("Client connection error occurred", t)
                    runningUserJob = false
                }

                override fun onClose() {
                    LOG.info("Client: Connection with id " + connection.id + " has been closed")
                    runningUserJob = false
                }
            })

            connection.addMessageListener { c: Idscp2Connection, data: ByteArray ->
                println("Received message: " + String(data, StandardCharsets.UTF_8))
            }

            connection.unlockMessaging()

            thread {
                // wait until connected
                while (!connection.isConnected) {
                    Thread.sleep(1000L)
                }

                while (runningUserJob) {
                    // read from stdin
                    println("You can now type in your message")
                    val data = readLine()

                    if (data.isNullOrBlank()) {
                        // close connection EOF
                        connection.close()
                    } else {
                        // send data to connection
                        connection.send(data.toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }

        }.exceptionally { t: Throwable? ->
            LOG.error("Client endpoint error occurred", t)
            null
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CommandlineTunnelClient::class.java)
    }
}