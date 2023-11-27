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

import de.fhg.aisec.ids.api.contracts.ContractUtils.SERIALIZER
import de.fhg.aisec.ids.camel.processors.Constants.IDS_HEADER
import de.fhg.aisec.ids.camel.processors.Constants.REQUESTED_ARTIFACT_KEY
import de.fraunhofer.iais.eis.ArtifactRequestMessage
import de.fraunhofer.iais.eis.ArtifactResponseMessageBuilder
import de.fraunhofer.iais.eis.RejectionMessageBuilder
import de.fraunhofer.iais.eis.RejectionReason
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component("artifactRequestProcessor")
class ArtifactRequestProcessor : Processor {
    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val artifactRequestMessage =
            exchange.message.getHeader(
                IDS_HEADER,
                ArtifactRequestMessage::class.java
            )
        val requestedArtifact = artifactRequestMessage.requestedArtifact

        // TODO: If transferContract doesn't match expected contract from database, send rejection!
        val usedContract =
            ProviderDB.artifactUrisMapped2ContractAgreements[
                Pair(requestedArtifact, UsageControlMaps.getExchangePeerIdentity(exchange))
            ]
        if (LOG.isDebugEnabled) {
            LOG.debug("Contract for requested Artifact found {}", usedContract)
        }

        // if artifact is available/authorised create response else create rejection message
        if (!ProviderDB.availableArtifactURIs.containsKey(requestedArtifact)) {
            createRejectionMessage(exchange, artifactRequestMessage, RejectionReason.NOT_FOUND)
        } else if (usedContract == null || !ProviderDB.contractAgreements.containsKey(usedContract)) {
            LOG.debug("Provider DB: {}", ProviderDB.artifactUrisMapped2ContractAgreements)
            createRejectionMessage(exchange, artifactRequestMessage, RejectionReason.NOT_AUTHORIZED)
        } else {
            // Proceed normally and send ArtifactResponseMessage
            ArtifactResponseMessageBuilder().run {
                _correlationMessage_(artifactRequestMessage.id)
                _transferContract_(usedContract)
                let {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it.build()))
                    }
                    exchange.message.setHeader(IDS_HEADER, it)
                }
            }
            exchange.setProperty(REQUESTED_ARTIFACT_KEY, requestedArtifact)
        }
    }

    private fun createRejectionMessage(
        exchange: Exchange,
        artifactRequestMessage: ArtifactRequestMessage,
        rejectionReason: RejectionReason
    ) {
        if (LOG.isDebugEnabled) {
            LOG.debug("Constructing RejectionMessage for requested artifact: {}", rejectionReason)
        }
        RejectionMessageBuilder()
            ._correlationMessage_(artifactRequestMessage.id)
            ._rejectionReason_(rejectionReason)
            .let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it.build()))
                }
                exchange.message.setHeader(IDS_HEADER, it)
            }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactRequestProcessor::class.java)
    }
}
