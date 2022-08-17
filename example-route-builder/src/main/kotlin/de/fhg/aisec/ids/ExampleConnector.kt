/*-
 * ========================LICENSE_START=================================
 * example-route-builder
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids

import org.apache.camel.support.jsse.KeyManagersParameters
import org.apache.camel.support.jsse.KeyStoreParameters
import org.apache.camel.support.jsse.SSLContextParameters
import org.apache.camel.support.jsse.TrustManagersParameters
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.nio.file.FileSystems

@SpringBootApplication
open class ExampleConnector : TrustedConnector() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<ExampleConnector>(*args)
        }

        fun getSslContext(): SSLContextParameters {
            return SSLContextParameters().apply {
                certAlias = "1.0.1"
                keyManagers = KeyManagersParameters().apply {
                    keyStore = KeyStoreParameters().apply {
                        resource = FileSystems.getDefault().getPath("etc", "consumer-keystore.p12").toFile().path
                        password = "password"
                    }
                }
                trustManagers = TrustManagersParameters().apply {
                    keyStore = KeyStoreParameters().apply {
                        resource = FileSystems.getDefault().getPath("etc", "truststore.p12").toFile().path
                        password = "password"
                    }
                }
            }
        }
    }
}
