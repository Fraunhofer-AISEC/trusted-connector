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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.fhg.aisec.ids.api.Constants;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for configurations in the connector.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/config/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/config")
public class ConfigApi {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigApi.class);
	
	@GET()
	@Path("list")
	public String get() {
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		
		// if config service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return new GsonBuilder().create().toJson(new HashMap<>());
		}
		
		Preferences prefs = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		HashMap<String, String> pMap = new HashMap<>();
		try {
			for (String key : prefs.keys()) {
				pMap.put(key, prefs.get(key,null));
			}
		} catch (BackingStoreException e) {
			LOG.error(e.getMessage(), e);
		}
		return new GsonBuilder().create().toJson(pMap);
	}

	@POST
	@OPTIONS
	@Path("set")
	@Consumes("application/json")
	public String set(String settings) {
		LOG.info("Received string " + settings);
		Map<String, String> result = new GsonBuilder().create().fromJson(settings, new TypeToken<HashMap<String, String>>() {}.getType());
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		
		// if preferences service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return "no preferences service";
		}
		
		// Store into preferences service
		Preferences idsConfig = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		if (idsConfig==null) {
			return "no preferences registered for pid " + Constants.PREFERENCES_ID;
		}
		
		for (Iterator<String> iterator = result.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String value = result.get(key);
			idsConfig.put(key, value);
		}
		try {
			idsConfig.flush();
			return "ok";
		} catch (BackingStoreException e) {
			LOG.error(e.getMessage(), e);
			return e.getMessage();
		}		
	}
}
