/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.fhg.aisec.ids.webconsole.connectionsettings.Settings;

final public class Helper {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigApi.class);
	
	public static String createDefaultJsonConfig() { // String connectionName) {
		Gson gson = new Gson();
		
		JsonObject config = new JsonObject();
		//config.addProperty("connection", connectionName);
		config.addProperty("integrityProtectionandVerification", "1");      // None
		config.addProperty("authentication", "1");							// None
		config.addProperty("serviceIsolation", "1");						// None
		config.addProperty("integrityProtectionVerificationScope", "1");	// None
		config.addProperty("appExecutionResources", "1");					// None
		config.addProperty("dataUsageControlSupport", "1");					// None
		config.addProperty("auditLogging", "1");							// None
		config.addProperty("localDataConfidentiality", "1");							// None
		
		
		return gson.toJson(config);
		
	}
	
	public static Settings convertToSettings(String jsonString)
    {
        try {
        	Gson gson = new Gson();
        	Settings sets = gson.fromJson(jsonString, Settings.class);
        	return sets;
        }catch(Exception e)
        {
        	e.printStackTrace();
            return null;
        }
    }
}
