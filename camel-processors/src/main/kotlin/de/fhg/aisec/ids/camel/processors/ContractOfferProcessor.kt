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

import de.fhg.aisec.ids.camel.processors.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.processors.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ContractAgreementBuilder
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder
import de.fraunhofer.iais.eis.ContractOffer
import de.fraunhofer.iais.eis.ContractOfferMessage
import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * This Processor handles a ContractResponseMessage and creates a ContractAgreementMessage.
 */
class ContractOfferProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractOfferMessage = exchange.message.getHeader(
            IDSCP2_HEADER, ContractOfferMessage::class.java
        )

        val contractOfferReceived = SERIALIZER.deserialize(
            exchange.message.getBody(String::class.java),
            ContractOffer::class.java
        )

        // if contract is denied send ContractRejectionMsg else send ContractAgreementMsg
        val contractOfferIsAccepted = true
        if (!contractOfferIsAccepted) {
            createContractRejectionMessage(exchange, contractOfferMessage.id)
        } else {
            ContractAgreementMessageBuilder().run {
                _correlationMessage_(contractOfferMessage.id)
                let {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Serialization Header: {}", SERIALIZER.serialize(it.build()))
                    }
                    exchange.message.setHeader(IDSCP2_HEADER, it)
                }
            }

            val contractAgreement = ContractAgreementBuilder()
                ._consumer_(contractOfferReceived.consumer)
                ._provider_(contractOfferReceived.provider)
                ._contractAnnex_(contractOfferReceived.contractAnnex)
                ._contractDate_(contractOfferReceived.contractDate)
                ._contractDocument_(contractOfferReceived.contractDocument)
                ._contractEnd_(contractOfferReceived.contractEnd)
                ._contractStart_(contractOfferReceived.contractStart)
                ._obligation_(contractOfferReceived.obligation)
                ._prohibition_(contractOfferReceived.prohibition)
                ._permission_(contractOfferReceived.permission)
                .build()

            UsageControlMaps.addContractAgreement(contractAgreement)
            if (LOG.isDebugEnabled) {
                LOG.debug("Consumer saved contract ${contractAgreement.id}")
            }

            SERIALIZER.serialize(contractAgreement).let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("ContractAgreement ID: {}", contractAgreement.id)
                    LOG.debug("Serialization body: {}", it)
                }
                exchange.message.body = it
            }
        }
    }

    private fun createContractRejectionMessage(exchange: Exchange, correlationId: URI) {
        if (LOG.isDebugEnabled) {
            LOG.debug("Constructing ContractRejectionMessage")
        }
        ContractRejectionMessageBuilder()
            ._correlationMessage_(correlationId)
            .let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialization Header: {}", SERIALIZER.serialize(it.build()))
                }
                exchange.message.setHeader(IDSCP2_HEADER, it)
            }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferProcessor::class.java)
    }
}
