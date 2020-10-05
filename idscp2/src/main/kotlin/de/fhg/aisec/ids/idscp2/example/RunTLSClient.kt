package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import java.nio.file.Paths
import java.util.*

object RunTLSClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val settings = Idscp2Settings.Builder()
                .setKeyStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/aisecconnector2-keystore.p12")).path))
                .setTrustStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/client-truststore_new.p12")).path))
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setRatTimeoutDelay(300)
                .build()
        val initiator = Idscp2ClientInitiator()
        initiator.init(settings)
    }
}