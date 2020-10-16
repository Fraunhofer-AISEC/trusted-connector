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

import org.apache.camel.Exchange
import org.apache.camel.Processor
import java.io.InputStream

class MultiPartInputProcessor : Processor {
    @Throws(Exception::class)
    override fun process(exchange: Exchange) {
        // Parse Multipart message
        val parser = MultiPartStringParser(exchange.getIn().getBody(InputStream::class.java))
        // Parser JSON Header (should be an InfoModel object)
        exchange.getIn().setHeader("idsMultipartHeader", parser.header)
        // Remove current Content-Type header before setting the new one
        exchange.getIn().removeHeader("Content-Type")
        // Copy Content-Type from payload part
        exchange.getIn().setHeader("Content-Type", parser.payloadContentType)
        // Populate body with extracted payload
        exchange.getIn().body = parser.payload
    }
}