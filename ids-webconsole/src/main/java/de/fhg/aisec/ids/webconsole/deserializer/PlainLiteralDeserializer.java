/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.deserializer;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import de.fraunhofer.iais.eis.util.PlainLiteral;

import java.io.IOException;

public class PlainLiteralDeserializer extends JsonDeserializer<PlainLiteral> {

    @Override
    public PlainLiteral deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String EMPTY_STRING = "";

        JsonNode node = p.readValueAsTree();
        String value = node.has("value") ? node.get("value").asText() : EMPTY_STRING;
        String language = node.has("language") ? node.get("language").asText() : EMPTY_STRING;
        return new PlainLiteral(value, language);
    }
	
}
