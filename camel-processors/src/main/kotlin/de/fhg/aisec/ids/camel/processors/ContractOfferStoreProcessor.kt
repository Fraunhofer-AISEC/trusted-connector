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
import de.fhg.aisec.ids.api.contracts.ContractManager
import de.fhg.aisec.ids.camel.processors.Constants.CONTRACT_STORE_KEY
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI

/**
 * This Processor handles a ContractRequestMessage and creates a ContractResponseMessage.
 */
@Component("contractOfferStoreProcessor")
class ContractOfferStoreProcessor(
    @Autowired private val contractManager: ContractManager
) : Processor {
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
            } ?: throw RuntimeException("No property \"artifactUri\" found in Exchange, cannot build contract!")

        exchange.getProperty(CONTRACT_STORE_KEY)?.toString()?.let {
            val contractProperties = ContractHelper.collectContractProperties(artifactUri, exchange)
            val contractOffer = contractManager.makeContract(contractProperties)
            contractManager.storeContract(it, contractOffer)
        } ?: throw RuntimeException("No store key for created contract provided!")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferStoreProcessor::class.java)
    }
}
