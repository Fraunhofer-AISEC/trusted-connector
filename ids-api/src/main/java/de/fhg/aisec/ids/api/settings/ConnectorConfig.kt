/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.api.settings

import java.io.Serializable

data class ConnectorConfig(
    val appstoreUrl: String = "https://raw.githubusercontent.com/industrial-data-space/templates/master/templates.json",
    val brokerUrl: String = "",
    val ttpHost: String = "",
    val ttpPort: Int = 443,
    val acmeServerWebcon: String = "",
    val acmeDnsWebcon: String = "",
    val acmePortWebcon: Int = 80,
    val tosAcceptWebcon: Boolean = false,
    val dapsUrl: String = "https://daps.aisec.fraunhofer.de/v2",
    val keystoreName: String = "provider-keystore.p12",
    val keystorePassword: String = "password",
    val keystoreAliasName: String = "1",
    val truststoreName: String = "truststore.p12"
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
