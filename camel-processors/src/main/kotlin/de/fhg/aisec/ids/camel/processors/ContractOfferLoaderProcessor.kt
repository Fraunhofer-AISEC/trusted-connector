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
import de.fraunhofer.iais.eis.ContractOfferMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * This Processor handles a ContractRequestMessage and creates a ContractResponseMessage.
 */
@Component("contractOfferLoaderProcessor")
class ContractOfferLoaderProcessor(@Autowired private val contractManager: ContractManager) : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        ContractOfferMessageBuilder().let {
            if (LOG.isDebugEnabled) {
                LOG.debug("Serialization header: {}", SERIALIZER.serialize(it.build()))
            }
            exchange.message.setHeader(IDS_HEADER, it)
        }

        val storeKey = exchange.getProperty(CONTRACT_STORE_KEY)?.toString()
        val contractOffer = storeKey?.let {
            contractManager.loadContract(it)
        } ?: throw RuntimeException("Error loading ContractOffer with store key \"$storeKey\"")

        SERIALIZER.serialize(contractOffer).let {
            if (LOG.isDebugEnabled) {
                LOG.debug("ContractOffer ID: {}", contractOffer.id)
                LOG.debug("Serialisation body: {}", it)
            }
            exchange.message.body = it
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferLoaderProcessor::class.java)
    }
}
