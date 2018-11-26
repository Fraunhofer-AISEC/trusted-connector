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
        val node = p.readValueAsTree<JsonNode>()

        val id = if (node.has("id")) node.get("id").asText() else ""
        val basedOn = if (node.has("basedOn")) node.get("basedOn").asText() else null
        val integrityProtectionAndVerification = if (node.has("integrityProtectionAndVerificationSupport"))
            node.get("integrityProtectionAndVerificationSupport").asText()
        else
            NONE
        val authenticationSupport = if (node.has("authenticationSupport"))
            node.get("authenticationSupport").asText()
        else
            NONE
        val serviceIsolationSupport = if (node.has("serviceIsolationSupport"))
            node.get("serviceIsolationSupport").asText()
        else
            NONE
        val integrityProtectionScope = if (node.has("integrityProtectionScope"))
            node.get("integrityProtectionScope").asText()
        else
            NONE
        val appExecutionResources = if (node.has("appExecutionResources"))
            node.get("appExecutionResources").asText()
        else
            NONE
        val dataUsageControlSupport = if (node.has("dataUsageControlSupport"))
            node.get("dataUsageControlSupport").asText()
        else
            NONE
        val auditLogging = if (node.has("auditLogging")) node.get("auditLogging").asText() else NONE
        val localDataConfidentiality = if (node.has("localDataConfidentiality"))
            node.get("localDataConfidentiality").asText()
        else
            NONE

        val psp: PredefinedSecurityProfile?
        if (basedOn != null) {
            psp = PredefinedSecurityProfile.valueOf(basedOn)
        } else {
            psp = null
        }

        try {
            val securityProfileBuilder: SecurityProfileBuilder
            if (!id.isEmpty()) {
                securityProfileBuilder = SecurityProfileBuilder(URL(id))
            } else {
                securityProfileBuilder = SecurityProfileBuilder()
            }
            return securityProfileBuilder._basedOn_(psp)
                    ._integrityProtectionAndVerificationSupport_(
                            IntegrityProtectionAndVerificationSupport
                                    .valueOf(integrityProtectionAndVerification))
                    ._authenticationSupport_(AuthenticationSupport.valueOf(authenticationSupport))
                    ._serviceIsolationSupport_(ServiceIsolationSupport.valueOf(serviceIsolationSupport))
                    ._integrityProtectionScope_(
                            IntegrityProtectionScope.valueOf(integrityProtectionScope))
                    ._appExecutionResources_(AppExecutionResources.valueOf(appExecutionResources))
                    ._dataUsageControlSupport_(DataUsageControlSupport.valueOf(dataUsageControlSupport))
                    ._auditLogging_(AuditLogging.valueOf(auditLogging))
                    ._localDataConfidentiality_(
                            LocalDataConfidentiality.valueOf(localDataConfidentiality))
                    .build()
        } catch (ex: ConstraintViolationException) {
            LOG.error("Caught ConstraintViolationException while deserializing Security profile.", ex)
            return null
        } catch (ex: MalformedURLException) {
            LOG.error("Caught MalformedURLException while deserializing Security profile.", ex)
            return null
        }

    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SecurityProfileDeserializer::class.java)
        private const val NONE = "NONE"
    }
}
