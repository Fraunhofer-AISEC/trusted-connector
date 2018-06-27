/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.ids.comm.ws.protocol.metadata;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;

public class MetadataHandler {
	protected static Logger LOG = LoggerFactory.getLogger(MetadataHandler.class);
	protected static String lastError = "";
	protected long sessionID = -1;
	protected String mySelfDescription = "";
	
	public String generateMetaDataRDF() {
		String rdfSelfDescription = "THIS IS SOME META DATA";
		
		return rdfSelfDescription;
	}
	
	public static MessageLite sendError(String lastError) {
		return ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage(lastError)
						.build())
				.build();
	}
}
