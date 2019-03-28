/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.aisec.camel.multipart;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.InputStream;

public class MultiPartInputProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		// Parse Multipart message
		MultiPartStringParser parser = new MultiPartStringParser(exchange.getIn().getBody(InputStream.class));
		// Parser JSON Header (should be an InfoModel object)
		exchange.getOut().setHeader("idsMultipartHeader", parser.getHeader());
		// Copy Content-Type from payload part
		exchange.getOut().setHeader("Content-Type", parser.getPayloadContentType());
		// Populate body with extracted payload
		exchange.getOut().setBody(parser.getPayload());
	}

}
