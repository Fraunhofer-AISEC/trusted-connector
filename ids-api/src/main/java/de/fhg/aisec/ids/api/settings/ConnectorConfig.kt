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

class ConnectorConfig : Serializable {
    val appstoreUrl = "https://raw.githubusercontent.com/industrial-data-space/templates/master/templates.json"
    val brokerUrl = ""
    val ttpHost = ""
    val ttpPort = 443
    val acmeServerWebcon = ""
    val acmeDnsWebcon = ""
    val acmePortWebcon = 80
    val tosAcceptWebcon = false
    val dapsUrl = "https://daps.aisec.fraunhofer.de"
    val keystoreName = "provider-keystore.p12"
    val keystorePassword = "password"
    val keystoreAliasName = "1"
    val truststoreName = "truststore.p12"

    companion object {
        private const val serialVersionUID = 1L
    }
}
