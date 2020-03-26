/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.aisec.ids.comm.ws.protocol.metadata;

import com.google.protobuf.MessageLite;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataHandler {
  protected static Logger LOG = LoggerFactory.getLogger(MetadataHandler.class);
  protected static String lastError = "";
  protected long sessionID = -1;
  protected String rdfSelfDescription = "THIS IS SOME META DATA";
  protected String dynamicAttributeToken = "INVALID_TOKEN";

  public MetadataHandler() {}

  public MetadataHandler(String rdfSelfDescription, String dynamicAttributeToken) {
    this.rdfSelfDescription = rdfSelfDescription;
    this.dynamicAttributeToken = dynamicAttributeToken;
  }

  public String getMetaData() {

    return this.rdfSelfDescription;
  }

  public String getDynamicAttributeToken() {
    return this.dynamicAttributeToken;
  }

  public static MessageLite sendError(String lastError) {
    return ConnectorMessage.newBuilder()
        .setId(0)
        .setType(ConnectorMessage.Type.ERROR)
        .setError(Error.newBuilder().setErrorCode("").setErrorMessage(lastError).build())
        .build();
  }
}
