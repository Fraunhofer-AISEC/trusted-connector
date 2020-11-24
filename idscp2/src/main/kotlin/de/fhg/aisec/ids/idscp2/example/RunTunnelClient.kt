package de.fhg.aisec.ids.idscp2.example

import de.fhg.aisec.ids.idscp2.default_drivers.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.default_drivers.rat.dummy.RatVerifierDummy
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

        val config = Idscp2Configuration.Builder()
                .setKeyStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/aisecconnector2-keystore.p12")).path))
                .setTrustStorePath(Paths.get(Objects.requireNonNull(RunTLSClient::class.java.classLoader.getResource("ssl/client-truststore_new.p12")).path))
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setAckTimeoutDelay(500) //  500 ms
                .setHandshakeTimeoutDelay(5 * 1000) // 5 seconds
                .setAttestationConfig(localAttestationConfig)
                .setServerPort(1234)
                .build()

        val initiator = CommandlineTunnelClient()
        initiator.init(config)
    }
}