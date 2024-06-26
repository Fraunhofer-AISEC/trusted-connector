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
package de.fhg.aisec.ids.api.conm

/**
 * Bean representing an "IDSCP Connection" .
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
class IDSCPIncomingConnection {
    var endpointIdentifier: String? = null
    var attestationResult: RatResult? = null
    var endpointKey: String? = null
    var remoteHostName: String? = null
    var metaData: String? = null
    private var dynamicAttributeToken: String? = null

    override fun toString(): String =
        (
            "IDSCPConnection [endpoint_identifier=" +
                endpointIdentifier +
                ", attestationResult=" +
                attestationResult +
                "]"
        )

    fun setDynamicAttributeToken(dynamicAttributeToken: String?) {
        this.dynamicAttributeToken = dynamicAttributeToken
    }
}
