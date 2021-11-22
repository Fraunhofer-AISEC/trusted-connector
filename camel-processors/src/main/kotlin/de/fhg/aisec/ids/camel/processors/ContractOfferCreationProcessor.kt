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

import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.processors.Constants.CONTAINER_URI_PROPERTY
import de.fhg.aisec.ids.camel.processors.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.processors.Utils.SERIALIZER
import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.ConstraintBuilder
import de.fraunhofer.iais.eis.ContractOfferBuilder
import de.fraunhofer.iais.eis.ContractOfferMessageBuilder
import de.fraunhofer.iais.eis.LeftOperand
import de.fraunhofer.iais.eis.PermissionBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * This Processor handles a ContractRequestMessage and creates a ContractResponseMessage.
 */
class ContractOfferCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        ContractOfferMessageBuilder().let {
            if (LOG.isDebugEnabled) {
                LOG.debug("Serialization header: {}", SERIALIZER.serialize(it.build()))
            }
            exchange.message.setHeader(IDSCP2_HEADER, it)
        }

        // create ContractOffer, allowing use of received data in the given container only
        val artifactUri = exchange.getProperty(Constants.ARTIFACT_URI_PROPERTY)?.let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        val containerUri = exchange.getProperty(CONTAINER_URI_PROPERTY).let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        val contractDate = Utils.createGregorianCalendarTimestamp(System.currentTimeMillis())
        val contractOffer = ContractOfferBuilder()
            ._contractDate_(contractDate)
            ._contractStart_(contractDate)
            // Contract end one year in the future
            ._contractEnd_(contractDate.apply { year += 1 })
            // Permission for data processing inside a specific system (docker container)
            ._permission_(
                listOf(
                    PermissionBuilder()
                        ._target_(artifactUri)
                        ._constraint_(
                            listOf(
                                ConstraintBuilder()
                                    ._leftOperand_(LeftOperand.SYSTEM)
                                    ._operator_(BinaryOperator.SAME_AS)
                                    ._rightOperandReference_(containerUri)
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .build()

        SERIALIZER.serialize(contractOffer).let {
            if (LOG.isDebugEnabled) {
                LOG.debug("ContractOffer ID: {}", contractOffer.id)
                LOG.debug("Serialisation body: {}", it)
            }
            exchange.message.body = it
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferCreationProcessor::class.java)
    }
}
