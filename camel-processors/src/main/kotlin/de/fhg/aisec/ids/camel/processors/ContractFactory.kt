/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
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

import de.fraunhofer.iais.eis.AbstractConstraint
import de.fraunhofer.iais.eis.Action
import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.ConstraintBuilder
import de.fraunhofer.iais.eis.ContractOffer
import de.fraunhofer.iais.eis.ContractOfferBuilder
import de.fraunhofer.iais.eis.LeftOperand
import de.fraunhofer.iais.eis.PermissionBuilder
import de.fraunhofer.iais.eis.util.TypedLiteral
import org.apache.camel.Exchange
import java.net.URI

object ContractFactory {

    fun buildContractOffer(requestedArtifact: URI, exchange: Exchange): ContractOffer {
        // Docker image whitelisting
        val dockerImageUris = (
            exchange.getProperty(Constants.UC_DOCKER_IMAGE_URIS)
                // Legacy property name without "uc-" prefix
                ?: exchange.getProperty("containerUri")
                ?: ""
            ).toString()
            .split(Regex("\\s+"))
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(URI::create)
            .toList()
        val contractDate = Utils.newGregorianCalendar()
        val timeConstraints = mutableListOf<AbstractConstraint>()
        // Add not after (BEFORE) usage constraint
        exchange.getProperty(Constants.UC_NOT_AFTER_DATETIME)?.let { dateTime ->
            timeConstraints += ConstraintBuilder()
                ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                ._operator_(BinaryOperator.BEFORE)
                ._rightOperand_(
                    TypedLiteral(
                        Utils.DATATYPE_FACTORY.newXMLGregorianCalendar(dateTime.toString()).toString(),
                        Utils.TYPE_DATETIMESTAMP
                    )
                )
                .build()
        }
        // Add not before (AFTER) usage constraint
        exchange.getProperty(Constants.UC_NOT_BEFORE_DATETIME)?.let { dateTime ->
            timeConstraints += ConstraintBuilder()
                ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                ._operator_(BinaryOperator.AFTER)
                ._rightOperand_(
                    TypedLiteral(
                        Utils.DATATYPE_FACTORY.newXMLGregorianCalendar(dateTime.toString()).toString(),
                        Utils.TYPE_DATETIMESTAMP
                    )
                )
                .build()
        }
        return ContractOfferBuilder()
            ._contractDate_(contractDate)
            ._contractStart_(contractDate)
            // Contract end one year in the future
            ._contractEnd_(contractDate.copy().apply { year += 1 })
            ._permission_(
                if (dockerImageUris.isEmpty()) {
                    // If no Docker images have been specified, just use time constraints
                    listOf(
                        PermissionBuilder()
                            ._target_(requestedArtifact)
                            ._action_(listOf(Action.USE))
                            ._constraint_(
                                timeConstraints
                            )
                            .build()
                    )
                } else {
                    // If Docker images have been specified, combine each with the specified time constraints
                    dockerImageUris.map {
                        PermissionBuilder()
                            ._target_(requestedArtifact)
                            ._action_(listOf(Action.USE))
                            ._constraint_(
                                listOf(
                                    ConstraintBuilder()
                                        ._leftOperand_(LeftOperand.SYSTEM)
                                        ._operator_(BinaryOperator.SAME_AS)
                                        ._rightOperandReference_(it)
                                        .build()
                                ) + timeConstraints
                            )
                            .build()
                    }
                }
            )
            .build()
    }
}
