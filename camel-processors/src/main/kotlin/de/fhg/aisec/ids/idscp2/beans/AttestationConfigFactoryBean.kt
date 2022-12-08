package de.fhg.aisec.ids.idscp2.beans

import de.fhg.aisec.ids.idscp2.api.configuration.AttestationConfig
import org.springframework.beans.factory.FactoryBean

@Suppress("unused")
class AttestationConfigFactoryBean : FactoryBean<AttestationConfig> {

    private val builder = AttestationConfig.Builder()

    fun setExpectedRaSuite(expectedSuite: String) =
        builder.setExpectedRaSuite(expectedSuite.split('|').toTypedArray())

    fun setSupportedRaSuite(supportedSuite: String) =
        builder.setSupportedRaSuite(supportedSuite.split('|').toTypedArray())

    fun setRaTimeoutDelay(delay: Long) = builder.setRaTimeoutDelay(delay)

    override fun getObject() = builder.build()

    override fun getObjectType() = AttestationConfig::class.java
}