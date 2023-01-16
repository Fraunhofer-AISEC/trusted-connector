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

import de.fhg.aisec.ids.camel.idscp2.Idscp2Endpoint
import de.fhg.aisec.ids.idscp2.defaultdrivers.securechannel.tls13.NativeTlsConfiguration
import org.apache.camel.support.jsse.SSLContextParameters
import org.springframework.beans.factory.FactoryBean

@Suppress("unused")
class NativeTlsConfigurationBuilderFactoryBean : FactoryBean<NativeTlsConfiguration.Builder> {

    private val builder = NativeTlsConfiguration.Builder()

    var sslParameters: SSLContextParameters
        set(value) {
            Idscp2Endpoint.applySslContextParameters(builder, value)
        }
        get() = throw UnsupportedOperationException("set-only Builder method")

    var serverSocketTimeout: Int
        set(value) {
            builder.setServerSocketTimeout(value)
        }
        get() = throw UnsupportedOperationException("set-only Builder method")

    override fun getObject() = builder

    override fun getObjectType() = NativeTlsConfiguration.Builder::class.java
}
