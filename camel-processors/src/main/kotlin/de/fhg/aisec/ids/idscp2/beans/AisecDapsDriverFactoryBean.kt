package de.fhg.aisec.ids.idscp2.beans

import de.fhg.aisec.ids.camel.idscp2.Idscp2Endpoint.Companion.applySslContextParameters
import de.fhg.aisec.ids.idscp2.daps.aisecdaps.AisecDapsDriver
import de.fhg.aisec.ids.idscp2.daps.aisecdaps.AisecDapsDriverConfig
import de.fhg.aisec.ids.idscp2.daps.aisecdaps.SecurityRequirements
import de.fhg.aisec.ids.idscp2.keystores.KeyStoreUtil.loadKeyStore
import org.apache.camel.support.jsse.SSLContextParameters
import org.springframework.beans.factory.FactoryBean
import java.nio.file.Paths
import javax.net.ssl.TrustManager

@Suppress("unused")
class AisecDapsDriverFactoryBean : FactoryBean<AisecDapsDriver> {

    private val builder = AisecDapsDriverConfig.Builder()

    fun setDapsUrl(dapsUrl: String) = builder.setDapsUrl(dapsUrl)

    fun setTrustManager(trustManager: TrustManager) = builder.setTrustManager(trustManager)

    fun setSecurityRequirements(securityRequirements: SecurityRequirements) =
        builder.setSecurityRequirements(securityRequirements)

    fun setTokenRenewalThreshold(threshold: Float) = builder.setTokenRenewalThreshold(threshold)

    fun setDapsSslParameters(sslContextParameters: SSLContextParameters) =
        applySslContextParameters(builder, sslContextParameters)

    fun setTransportCertificatesParameters(sslContextParameters: SSLContextParameters): AisecDapsDriverConfig.Builder {
        val ks = loadKeyStore(
            sslContextParameters.keyManagers.keyStore.resource.let { Paths.get(it) }
                ?: throw RuntimeException("Error loading transport certificates: No KeyStore file provided!"),
            sslContextParameters.keyManagers.keyStore.password?.toCharArray()
                ?: throw RuntimeException("Error loading transport certificates: No KeyStore file provided!")
        )
        return builder.loadTransportCertsFromKeystore(ks)
    }

    override fun getObject() = AisecDapsDriver(builder.build())

    override fun getObjectType() = AisecDapsDriver::class.java
}