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
import de.fhg.aisec.ids.camel.processors.Constants.IDS_TYPE
import de.fraunhofer.iais.eis.ArtifactRequestMessage
import de.fraunhofer.iais.eis.ArtifactResponseMessage
import de.fraunhofer.iais.eis.ContractAgreementMessage
import de.fraunhofer.iais.eis.ContractOfferMessage
import de.fraunhofer.iais.eis.ContractRejectionMessage
import de.fraunhofer.iais.eis.ContractRequestMessage
import de.fraunhofer.iais.eis.ContractResponseMessage
import de.fraunhofer.iais.eis.DescriptionRequestMessage
import de.fraunhofer.iais.eis.DescriptionResponseMessage
import de.fraunhofer.iais.eis.LogMessage
import de.fraunhofer.iais.eis.Message
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage
import de.fraunhofer.iais.eis.QueryMessage
import de.fraunhofer.iais.eis.RejectionMessage
import de.fraunhofer.iais.eis.RequestMessage
import de.fraunhofer.iais.eis.ResourceUpdateMessage
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class IdsMessageTypeExtractionProcessor : Processor {
    override fun process(exchange: Exchange) {
        processHeader(exchange)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IdsMessageTypeExtractionProcessor::class.java)

        fun processHeader(exchange: Exchange) {
            if (LOG.isDebugEnabled) {
                LOG.debug("[IN] ${IdsMessageTypeExtractionProcessor::class.java.simpleName}")
            }
            exchange.message.getHeader(IDSCP2_HEADER, Message::class.java)?.let { header ->
                val messageType = when (header) {
                    is ArtifactRequestMessage -> ArtifactRequestMessage::class.simpleName
                    is ArtifactResponseMessage -> ArtifactResponseMessage::class.simpleName
                    is ContractRequestMessage -> ContractRequestMessage::class.simpleName
                    is ContractResponseMessage -> ContractResponseMessage::class.simpleName
                    is ContractOfferMessage -> ContractOfferMessage::class.simpleName
                    is ContractAgreementMessage -> ContractAgreementMessage::class.simpleName
                    is ContractRejectionMessage -> ContractRejectionMessage::class.simpleName
                    is ResourceUpdateMessage -> ResourceUpdateMessage::class.simpleName
                    is MessageProcessedNotificationMessage -> MessageProcessedNotificationMessage::class.simpleName
                    is DescriptionRequestMessage -> DescriptionRequestMessage::class.simpleName
                    is DescriptionResponseMessage -> DescriptionResponseMessage::class.simpleName
                    is RejectionMessage -> RejectionMessage::class.simpleName
                    is LogMessage -> LogMessage::class.simpleName
                    is QueryMessage -> QueryMessage::class.simpleName
                    is RequestMessage -> RequestMessage::class.simpleName
                    else -> header::class.simpleName
                }
                if (LOG.isDebugEnabled) {
                    LOG.debug("Detected ids-type: {}", messageType)
                }
                exchange.setProperty(IDS_TYPE, messageType)
            }
        }
    }
}
