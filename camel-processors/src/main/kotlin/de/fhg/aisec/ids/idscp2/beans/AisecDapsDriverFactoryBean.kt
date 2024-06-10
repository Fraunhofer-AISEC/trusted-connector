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

import de.fhg.aisec.ids.camel.idscp2.applySslContextParameters
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

    var dapsUrl: String by BeanSetter(builder::setDapsUrl)

    var trustManager: TrustManager by BeanSetter(builder::setTrustManager)

    var securityRequirements: SecurityRequirements by BeanSetter(builder::setSecurityRequirements)

    var tokenRenewalThreshold: Float by BeanSetter(builder::setTokenRenewalThreshold)

    var dapsSslParameters: SSLContextParameters by BeanSetter(builder::applySslContextParameters)

    var transportCertificatesParameters: SSLContextParameters by BeanSetter {
        val ks =
            loadKeyStore(
                it.keyManagers.keyStore.resource
                    .let(Paths::get)
                    ?: throw RuntimeException("Error loading transport certificates: No KeyStore file provided!"),
                it.keyManagers.keyStore.password
                    ?.toCharArray()
                    ?: throw RuntimeException("Error loading transport certificates: No KeyStore password provided!")
            )
        builder.loadTransportCertsFromKeystore(ks)
    }

    override fun getObject() = AisecDapsDriver(builder.build())

    override fun getObjectType() = AisecDapsDriver::class.java
}
