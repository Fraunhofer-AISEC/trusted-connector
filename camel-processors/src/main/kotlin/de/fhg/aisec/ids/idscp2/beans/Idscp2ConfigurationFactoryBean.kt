/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.idscp2.beans

import de.fhg.aisec.ids.idscp2.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.api.drivers.DapsDriver
import org.springframework.beans.factory.FactoryBean

@Suppress("unused")
class Idscp2ConfigurationFactoryBean : FactoryBean<Idscp2Configuration> {
    private val builder = Idscp2Configuration.Builder()

    var attestationConfig: AttestationConfig by BeanSetter(builder::setAttestationConfig)

    var dapsDriver: DapsDriver by BeanSetter(builder::setDapsDriver)

    var handshakeTimeoutDelay: Long by BeanSetter(builder::setHandshakeTimeoutDelay)

    var ackTimeoutDelay: Long by BeanSetter(builder::setAckTimeoutDelay)

    override fun getObject() = builder.build()

    override fun getObjectType() = Idscp2Configuration::class.java
}
