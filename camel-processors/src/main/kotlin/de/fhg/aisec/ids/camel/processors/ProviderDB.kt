/*-
 * ========================LICENSE_START=================================
 * camel-processors
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
package de.fhg.aisec.ids.camel.processors

import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fraunhofer.iais.eis.ContractAgreement
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object ProviderDB {
    val availableArtifactURIs: ConcurrentHashMap<URI, String> = ConcurrentHashMap()
    val artifactUrisMapped2ContractAgreements:
        ConcurrentHashMap<Pair<URI, AppLayerConnection>, URI> = ConcurrentHashMap()
    val contractAgreements: ConcurrentHashMap<URI, ContractAgreement> = ConcurrentHashMap()

    init {
        availableArtifactURIs[URI.create("https://example.com/some_artifact")] = "AVAILABLE"
    }
}
