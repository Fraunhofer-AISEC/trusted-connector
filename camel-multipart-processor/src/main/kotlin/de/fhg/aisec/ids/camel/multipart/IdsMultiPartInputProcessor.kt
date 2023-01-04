/*-
 * ========================LICENSE_START=================================
 * camel-multipart-processor
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.camel.multipart

import de.fhg.aisec.ids.camel.multipart.MultiPartConstants.IDS_HEADER_KEY
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.stereotype.Component
import java.io.InputStream

@Component("idsMultiPartInputProcessor")
class IdsMultiPartInputProcessor : Processor {
    @Throws(Exception::class)
    override fun process(exchange: Exchange) {
        exchange.message.let {
            // Parse Multipart message
            val parser = MultiPartStringParser(it.getBody(InputStream::class.java))
            // Parser JSON Header (should be an InfoModel object)
            it.setHeader(IDS_HEADER_KEY, parser.header)
            // Remove current Content-Type header before setting the new one
            it.removeHeader("Content-Type")
            // Copy Content-Type from payload part
            it.setHeader("Content-Type", parser.payloadContentType)
            // Populate body with extracted payload
            it.body = parser.payload
        }
    }
}
