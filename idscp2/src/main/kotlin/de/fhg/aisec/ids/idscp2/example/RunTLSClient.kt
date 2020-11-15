package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration
import java.nio.file.Paths
import java.util.*

object RunTLSClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val localAttestationConfig = AttestationConfig.Builder()
                .setSupportedRatSuite(arrayOf("Dummy"))
                .setExpectedRatSuite(arrayOf("Dummy"))
                .setRatTimeoutDelay(300 * 1000) // 300 seconds
                .build()

        val settings = Idscp2Configuration.Builder()
                .setKeyStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/aisecconnector1-keystore.p12")).path))
                .setTrustStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/client-truststore_new.p12")).path))
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setAckTimeoutDelay(500) //  500 ms
                .setHandshakeTimeoutDelay(5 * 1000) // 5 seconds
                .setAttestationConfig(localAttestationConfig)
                .build()

        val initiator = Idscp2ClientInitiator()
        initiator.init(settings)
    }
}