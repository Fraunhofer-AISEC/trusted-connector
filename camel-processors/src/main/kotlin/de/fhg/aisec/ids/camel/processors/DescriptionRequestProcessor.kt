/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2023 Fraunhofer AISEC
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

import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.camel.processors.Constants.IDS_HEADER
import de.fraunhofer.iais.eis.DescriptionRequestMessage
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("descriptionRequestProcessor")
class DescriptionRequestProcessor : Processor {
    @Autowired
    private lateinit var infoModel: InfoModel

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val descriptionRequestMessage = exchange.message.getHeader(IDS_HEADER, DescriptionRequestMessage::class.java)

        DescriptionResponseMessageBuilder()
            ._correlationMessage_(descriptionRequestMessage.id)
            .let { exchange.message.setHeader(IDS_HEADER, it) }

        exchange.message.body = infoModel.connectorAsJsonLd
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DescriptionRequestProcessor::class.java)
    }
}
