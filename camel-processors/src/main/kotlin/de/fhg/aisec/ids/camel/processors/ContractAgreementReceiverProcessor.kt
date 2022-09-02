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
import de.fraunhofer.iais.eis.ContractAgreement
import de.fraunhofer.iais.eis.ContractAgreementMessage
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class ContractAgreementReceiverProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractAgreementMessage = exchange.message.getHeader(
            IDSCP2_HEADER,
            ContractAgreementMessage::class.java
        )

        val contractAgreement = SERIALIZER.deserialize(
            exchange.message.getBody(String::class.java),
            ContractAgreement::class.java
        )

        UsageControlMaps.addContractAgreement(contractAgreement)
        if (LOG.isDebugEnabled) {
            LOG.debug("Provider is saving contract ${contractAgreement.id}")
        }

        ProviderDB.contractAgreements[contractAgreement.id] = contractAgreement
        for (permission in contractAgreement.permission) {
            // Entry for the case when provider is setting up multiple agreements with different requesting connectors
            ProviderDB.artifactUrisMapped2ContractAgreements[
                Pair(
                    permission.target,
                    UsageControlMaps.getExchangeConnection(exchange)
                        ?: throw RuntimeException(
                            "No connection for exchange $exchange, " +
                                "this should never happen!"
                        )
                )
            ] = contractAgreement.id
            // Additional entry for the case when an artifact is pushed directly to the consumer
            ProviderDB.artifactUrisMapped2ContractAgreements[Pair(permission.target, null)] = contractAgreement.id
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("Saved Agreement {}", contractAgreement.id)
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Received ContractAgreementMessage {}", SERIALIZER.serialize(contractAgreementMessage))
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractAgreementReceiverProcessor::class.java)
    }
}
