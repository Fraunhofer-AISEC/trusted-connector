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

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;


import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SecurityProfileDeserializer extends JsonDeserializer<SecurityProfile>{
    private static final Logger LOG = LoggerFactory.getLogger(SecurityProfileDeserializer.class);

    // profile id will be automatically generated if not given
    // all attributes default to "NONE" if not specified
    @Override
    public SecurityProfile deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String NONE_STRING = "NONE";
        String EMPTY_STRING = "";

        JsonNode node = p.readValueAsTree();

        String id = node.has("id") ? node.get("id").asText() : EMPTY_STRING;
        String basedOn = node.has("basedOn") ? node.get("basedOn").asText(): null;
        String integrityProtectionAndVerification = node.has("integrityProtectionAndVerification") ? node.get("integrityProtectionAndVerification").asText() : NONE_STRING;
        String authenticationSupport = node.has("authenticationSupport") ? node.get("authenticationSupport").asText() : NONE_STRING;
        String serviceIsolationSupport = node.has("serviceIsolationSupport") ? node.get("serviceIsolationSupport").asText() : NONE_STRING;
        String integrityProtectionScope = node.has("integrityProtectionScope") ? node.get("integrityProtectionScope").asText() : NONE_STRING;
        String appExecutionResources = node.has("appExecutionResources") ? node.get("appExecutionResources").asText() : NONE_STRING;
        String dataUsageControlSupport = node.has("dataUsageControlSupport") ? node.get("dataUsageControlSupport").asText() : NONE_STRING;
        String auditLogging = node.has("auditLogging") ? node.get("auditLogging").asText() : NONE_STRING;
        String localDataConfidentiality = node.has("localDataConfidentiality") ? node.get("localDataConfidentiality").asText() : NONE_STRING;

        PredefinedSecurityProfile psp;
        if(basedOn!=null)
            psp =  PredefinedSecurityProfile.getByString(basedOn);
        else
            psp = null;
        
        try {
            if(!id.equals(EMPTY_STRING))
			    return new SecurityProfileBuilder(new URL(id)).basedOn(psp)
					.integrityProtectionAndVerification(IntegrityProtectionAndVerification
							.valueOf(integrityProtectionAndVerification))
					.authenticationSupport(AuthenticationSupport.valueOf(authenticationSupport))
					.serviceIsolationSupport(ServiceIsolationSupport.valueOf(serviceIsolationSupport))
					.integrityProtectionScope(IntegrityProtectionScope.valueOf(integrityProtectionScope))
					.appExecutionResources(AppExecutionResources.valueOf(appExecutionResources))
					.dataUsageControlSupport(DataUsageControlSupport.valueOf(dataUsageControlSupport))
					.auditLogging(AuditLogging.valueOf(auditLogging))
					.localDataConfidentiality(idsLocalDataConfidentiality.valueOf(localDataConfidentiality))
					.build();
            else
                return new SecurityProfileBuilder().basedOn(psp)                 //id automatically generated
                        .integrityProtectionAndVerification(IntegrityProtectionAndVerification
                                .valueOf(integrityProtectionAndVerification))
                        .authenticationSupport(AuthenticationSupport.valueOf(authenticationSupport))
                        .serviceIsolationSupport(ServiceIsolationSupport.valueOf(serviceIsolationSupport))
                        .integrityProtectionScope(IntegrityProtectionScope.valueOf(integrityProtectionScope))
                        .appExecutionResources(AppExecutionResources.valueOf(appExecutionResources))
                        .dataUsageControlSupport(DataUsageControlSupport.valueOf(dataUsageControlSupport))
                        .auditLogging(AuditLogging.valueOf(auditLogging))
                        .localDataConfidentiality(idsLocalDataConfidentiality.valueOf(localDataConfidentiality))
                        .build();
		} catch (ConstraintViolationException ex) {
            LOG.error("Caught ConstraintViolationException while deserializing Security profile.", ex);
            return null;
        } catch (MalformedURLException ex) {
            LOG.error("Caught MalformedURLException while deserializing Security profile.", ex);
            return null;
        }
    }
	
}
