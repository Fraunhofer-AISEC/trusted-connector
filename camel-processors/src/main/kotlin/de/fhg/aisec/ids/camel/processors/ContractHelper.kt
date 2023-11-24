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

import de.fhg.aisec.ids.api.contracts.ContractConstants
import de.fhg.aisec.ids.api.contracts.ContractUtils
import de.fraunhofer.iais.eis.ContractAgreementBuilder
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder
import de.fraunhofer.iais.eis.ContractOffer
import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder
import org.apache.camel.Exchange
import org.slf4j.Logger
import java.net.URI

object ContractHelper {
    fun collectContractProperties(
        requestedArtifact: URI,
        exchange: Exchange
    ): Map<String, Any> {
        val contractProperties =
            mutableMapOf<String, Any>(
                ContractConstants.ARTIFACT_URI_PROPERTY to requestedArtifact
            )
        // Docker image whitelisting
        contractProperties[ContractConstants.UC_DOCKER_IMAGE_URIS] =
            (
                exchange.getProperty(ContractConstants.UC_DOCKER_IMAGE_URIS)
                    // Legacy property name without "uc-" prefix
                    ?: exchange.getProperty("containerUri")
                    ?: ""
            ).toString()
                .split(Regex("\\s+"))
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map(URI::create)
                .toList()
        // Add not after (BEFORE) usage constraint
        exchange.getProperty(ContractConstants.UC_NOT_AFTER_DATETIME)?.let {
            contractProperties[ContractConstants.UC_NOT_AFTER_DATETIME] = it
        }
        // Add not before (AFTER) usage constraint
        exchange.getProperty(ContractConstants.UC_NOT_BEFORE_DATETIME)?.let {
            contractProperties[ContractConstants.UC_NOT_BEFORE_DATETIME] = it
        }
        return contractProperties
    }

    fun handleContractOffer(
        exchange: Exchange,
        correlationId: URI,
        logger: Logger
    ) {
        val contractOfferReceived =
            ContractUtils.SERIALIZER.deserialize(
                exchange.message.getBody(String::class.java),
                ContractOffer::class.java
            )

        // if contract is denied send ContractRejectionMsg else send ContractAgreementMsg
        val contractOfferIsAccepted = true
        if (!contractOfferIsAccepted) {
            createContractRejectionMessage(exchange, correlationId, logger)
        } else {
            ContractAgreementMessageBuilder().run {
                _correlationMessage_(correlationId)
                let {
                    if (logger.isDebugEnabled) {
                        logger.debug("Serialization Header: {}", ContractUtils.SERIALIZER.serialize(it.build()))
                    }
                    exchange.message.setHeader(Constants.IDS_HEADER, it)
                }
            }

            val contractAgreement =
                ContractAgreementBuilder()
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
            if (logger.isDebugEnabled) {
                logger.debug("Consumer saved contract ${contractAgreement.id}")
            }

            ContractUtils.SERIALIZER.serialize(contractAgreement).let {
                if (logger.isDebugEnabled) {
                    logger.debug("ContractAgreement ID: {}", contractAgreement.id)
                    logger.debug("Serialization body: {}", it)
                }
                exchange.message.body = it
            }
        }
    }

    private fun createContractRejectionMessage(
        exchange: Exchange,
        correlationId: URI,
        logger: Logger
    ) {
        if (logger.isDebugEnabled) {
            logger.debug("Constructing ContractRejectionMessage")
        }
        ContractRejectionMessageBuilder()
            ._correlationMessage_(correlationId)
            .let {
                if (logger.isDebugEnabled) {
                    logger.debug("Serialization Header: {}", ContractUtils.SERIALIZER.serialize(it.build()))
                }
                exchange.message.setHeader(Constants.IDS_HEADER, it)
            }
    }
}
