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

import de.fhg.aisec.ids.api.contracts.ContractConstants
import de.fhg.aisec.ids.api.contracts.ContractUtils.SERIALIZER
import de.fhg.aisec.ids.camel.processors.Constants.IDS_HEADER
import de.fraunhofer.iais.eis.ResourceUpdateMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

@Component("resourceUpdateCreationProcessor")
class ResourceUpdateCreationProcessor : Processor {
    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val artifactUri =
            exchange.getProperty(ContractConstants.ARTIFACT_URI_PROPERTY)?.let {
                if (it is URI) {
                    it
                } else {
                    URI.create(it.toString())
                }
            }

        val usedContract =
            ProviderDB.artifactUrisMapped2ContractAgreements[
                Pair(artifactUri, UsageControlMaps.getExchangePeerIdentity(exchange))
            ]
                ?: throw RuntimeException("No UC contract found for resource/artifact $artifactUri")
        if (LOG.isDebugEnabled) {
            LOG.debug("Contract for requested Artifact found {}", usedContract)
        }

        ResourceUpdateMessageBuilder()
            ._affectedResource_(artifactUri)
            ._transferContract_(usedContract)
            .let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it.build()))
                }
                exchange.message.setHeader(IDS_HEADER, it)
            }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ResourceUpdateCreationProcessor::class.java)
    }
}
