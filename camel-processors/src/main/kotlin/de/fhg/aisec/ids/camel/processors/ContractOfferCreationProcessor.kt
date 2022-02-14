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

import de.fhg.aisec.ids.camel.processors.Constants.CONTAINER_URI_PROPERTY
import de.fhg.aisec.ids.camel.processors.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.processors.Utils.SERIALIZER
import de.fhg.aisec.ids.camel.processors.Utils.TYPE_DATETIMESTAMP
import de.fraunhofer.iais.eis.AbstractConstraint
import de.fraunhofer.iais.eis.Action
import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.ConstraintBuilder
import de.fraunhofer.iais.eis.ContractOfferBuilder
import de.fraunhofer.iais.eis.ContractOfferMessageBuilder
import de.fraunhofer.iais.eis.LeftOperand
import de.fraunhofer.iais.eis.PermissionBuilder
import de.fraunhofer.iais.eis.util.TypedLiteral
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI
import de.fhg.aisec.ids.camel.idscp2.Utils as Idscp2Utils

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

        val constraints = mutableListOf<AbstractConstraint>()
        // create ContractOffer, allowing use of received data in the given container only
        val artifactUri = exchange.getProperty(Constants.ARTIFACT_URI_PROPERTY)?.let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        exchange.getProperty(CONTAINER_URI_PROPERTY).let {
            val containerUri = if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
            constraints += ConstraintBuilder()
                ._leftOperand_(LeftOperand.SYSTEM)
                ._operator_(BinaryOperator.SAME_AS)
                ._rightOperandReference_(containerUri)
                .build()
        }
        val contractDate = Idscp2Utils.createGregorianCalendarTimestamp(System.currentTimeMillis())
        constraints += ConstraintBuilder()
            ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
            ._operator_(BinaryOperator.LT)
            ._rightOperand_(
                TypedLiteral(
                    contractDate.apply {
                        add(Utils.newDuration(3_600_000))
                    }.toString(),
                    TYPE_DATETIMESTAMP
                )
            )
            .build()
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
                        ._action_(listOf(Action.USE))
                        ._constraint_(constraints)
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
