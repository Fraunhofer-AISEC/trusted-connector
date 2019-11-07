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
package de.fhg.aisec.ids.informationmodelmanager.deserializer


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import de.fraunhofer.iais.eis.*
import de.fraunhofer.iais.eis.util.ConstraintViolationException
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL


class SecurityProfileDeserializer : JsonDeserializer<SecurityProfile>() {

    /**
     * Profile id will be automatically generated if not given
     * all attributes default to "NONE" if not specified
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SecurityProfile? {
        return try {
            ctxt.readValue(p, SecurityProfile::class.java)
        } catch (x: Exception) {
            LOG.error("Error during SecurityProfile parsing, falling back to BASE_CONNECTOR_SECURITY_PROFILE", x)
            SecurityProfile.BASE_CONNECTOR_SECURITY_PROFILE;
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SecurityProfileDeserializer::class.java)
    }
}
