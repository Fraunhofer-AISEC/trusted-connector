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
package de.fhg.aisec.ids.api.external

import de.fhg.aisec.ids.messages.BrokerProtos.AnnounceServiceResponse
import de.fhg.aisec.ids.messages.BrokerProtos.ServiceDescription
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse
import java.nio.channels.Channel
import java.util.concurrent.CompletableFuture

/**
 * Interface of an IDS connector towards the IDS network, i.e. outside of the connector itself.
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de), Gerd Brost
 */
interface ExternalAPI {
    fun addEndpoint(s: String, channel: Channel)
    fun hasEndpoint(s: String): Boolean

    /**
     * Method to announce a service to a broker
     *
     * @param description Description of the service to be accounced
     * @return Service accouncement response
     */
    fun announceService(description: ServiceDescription): CompletableFuture<AnnounceServiceResponse>

    /**
     * Method to request attestation
     *
     * @param URI URI of the connecter that is to be verified
     * @param nonce Nonce for freshness proof
     * @return Returns the response of the attestation process
     */
    fun requestAttestation(URI: String, nonce: Int): CompletableFuture<AttestationResponse>
}
