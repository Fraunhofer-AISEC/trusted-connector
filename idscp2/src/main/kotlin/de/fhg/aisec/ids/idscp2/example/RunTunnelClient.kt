package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.default_drivers.daps.NullDaps
import de.fhg.aisec.ids.idscp2.default_drivers.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.default_drivers.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.tlsv1_3.NativeTlsConfiguration
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import java.nio.file.Paths
import java.util.*

object RunTunnelClient {
    @JvmStatic
    fun main(args: Array<String>) {

        val localAttestationConfig = AttestationConfig.Builder()
                .setSupportedRatSuite(arrayOf(RatProverDummy.RAT_PROVER_DUMMY_ID))
                .setExpectedRatSuite(arrayOf(RatVerifierDummy.RAT_VERIFIER_DUMMY_ID))
                .setRatTimeoutDelay(70 * 1000) // 70 seconds
                .build()

        // create daps driver
        val dapsDriver = NullDaps()

        val config = Idscp2Configuration.Builder()
                .setAckTimeoutDelay(500) //  500 ms
                .setHandshakeTimeoutDelay(5 * 1000) // 5 seconds
                .setAttestationConfig(localAttestationConfig)
                .setDapsDriver(dapsDriver)
                .build()

        val nativeTlsConfiguration = NativeTlsConfiguration.Builder()
                .setKeyStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/consumer-keystore.p12")).path))
                .setTrustStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/truststore.p12")).path))
                .setCertificateAlias("1.0.1")
                .setServerPort(12345)
                .setHost("provider-core")
                .build()

        val initiator = CommandlineTunnelClient()
        initiator.init(config, nativeTlsConfiguration)
    }
}