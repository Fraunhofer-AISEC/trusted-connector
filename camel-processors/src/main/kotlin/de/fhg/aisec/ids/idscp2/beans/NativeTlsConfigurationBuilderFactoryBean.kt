package de.fhg.aisec.ids.idscp2.beans

import de.fhg.aisec.ids.camel.idscp2.Idscp2Endpoint
import de.fhg.aisec.ids.idscp2.defaultdrivers.securechannel.tls13.NativeTlsConfiguration
import org.apache.camel.support.jsse.SSLContextParameters
import org.springframework.beans.factory.FactoryBean

@Suppress("unused")
class NativeTlsConfigurationBuilderFactoryBean : FactoryBean<NativeTlsConfiguration.Builder> {

    private val builder = NativeTlsConfiguration.Builder()

    fun setSslParameters(sslContextParameters: SSLContextParameters) =
        Idscp2Endpoint.applySslContextParameters(builder, sslContextParameters)

    fun setServerSocketTimeout(timeout: Int) = builder.setServerSocketTimeout(timeout)

    override fun getObject() = builder

    override fun getObjectType() = NativeTlsConfiguration.Builder::class.java
}