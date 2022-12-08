package de.fhg.aisec.ids.idscp2.beans

import de.fhg.aisec.ids.idscp2.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.api.drivers.DapsDriver
import org.springframework.beans.factory.FactoryBean

@Suppress("unused")
class Idscp2ConfigurationFactoryBean : FactoryBean<Idscp2Configuration> {

    private val builder = Idscp2Configuration.Builder()

    fun setAttestationConfig(config: AttestationConfig) = builder.setAttestationConfig(config)

    fun setDapsDriver(dapsDriver: DapsDriver) = builder.setDapsDriver(dapsDriver)

    fun setHandshakeTimeoutDelay(delay: Long) = builder.setHandshakeTimeoutDelay(delay)

    fun setAckTimeoutDelay(delay: Long) = builder.setAckTimeoutDelay(delay)

    override fun getObject() = builder.build()

    override fun getObjectType() = Idscp2Configuration::class.java
}