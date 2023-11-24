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

import de.fhg.aisec.ids.api.contracts.ContractManager
import de.fhg.aisec.ids.api.contracts.ContractUtils.SERIALIZER
import de.fhg.aisec.ids.camel.processors.Constants.CONTRACT_STORE_KEY
import de.fhg.aisec.ids.camel.processors.Constants.IDS_HEADER
import de.fraunhofer.iais.eis.ContractRequest
import de.fraunhofer.iais.eis.ContractRequestMessage
import de.fraunhofer.iais.eis.ContractResponseMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This Processor handles a ContractRequestMessage and creates a ContractResponseMessage.
 */
@Component("contractRequestProcessor")
class ContractRequestProcessor(
    @Autowired private val contractManager: ContractManager
) : Processor {
    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractRequest =
            SERIALIZER.deserialize(
                exchange.message.getBody(String::class.java),
                ContractRequest::class.java
            )
        val requestedArtifact = contractRequest.permission[0].target

        val contractRequestMessage =
            exchange.message.getHeader(
                IDS_HEADER,
                ContractRequestMessage::class.java
            )

        ContractResponseMessageBuilder()
            ._correlationMessage_(contractRequestMessage.id)
            .let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialization header: {}", SERIALIZER.serialize(it.build()))
                }
                exchange.message.setHeader(IDS_HEADER, it)
            }

        val storeKey = exchange.getProperty(CONTRACT_STORE_KEY)?.toString()
        val contractOffer =
            storeKey?.let {
                contractManager.loadContract(it)?.let { offer ->
                    if (offer.permission.none { p -> p.target == requestedArtifact }) {
                        throw RuntimeException(
                            "Offer with store key \"$it\"" +
                                " does not contain any permissions for artifact \"$requestedArtifact\""
                        )
                    }
                    offer
                } ?: throw RuntimeException("Error loading ContractOffer using store key \"$storeKey\"")
            } ?: run {
                val contractProperties = ContractHelper.collectContractProperties(requestedArtifact, exchange)
                contractManager.makeContract(contractProperties)
            }

        SERIALIZER.serialize(contractOffer).let {
            if (LOG.isDebugEnabled) {
                LOG.debug("ContractOffer ID: {}", contractOffer.id)
                LOG.debug("Serialisation body: {}", it)
            }
            exchange.message.body = it
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractRequestProcessor::class.java)
    }
}
