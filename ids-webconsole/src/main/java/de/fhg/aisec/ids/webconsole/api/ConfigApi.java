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

import com.google.gson.Gson;
import de.fhg.aisec.ids.api.Constants;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.data.ConnectionSettings;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * REST API interface for configurations in the connector.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/config/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
@Path("/config")
public class ConfigApi {
	public final static String GENERAL_CONFIG = "General Configuration";
	private static final Logger LOG = LoggerFactory.getLogger(ConfigApi.class);
	
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> get() {
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		if (!cO.isPresent()) {
			throw new ServiceUnavailableException("PreferenceService not available");
		}
		
		Preferences prefs = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		if (prefs == null) {
			return new HashMap<>();
		}
		
		HashMap<String, String> pMap = new HashMap<>();
		try {
			for (String key : prefs.keys()) {
				pMap.put(key, prefs.get(key, null));
			}
		} catch (BackingStoreException e) {
			LOG.error(e.getMessage(), e);
		}
		return pMap;
	}

	@POST
	@OPTIONS
	@Path("set")
	@Consumes(MediaType.APPLICATION_JSON)
	public String set(Map<String, String> settings) {
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		if (!cO.isPresent()) {
			throw new ServiceUnavailableException("PreferenceService not available");
		}
		
		if (settings == null) {
			throw new NotFoundException("Settings not found");
		}
		
		// Store into preferences service
		Preferences idsConfig = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		if (idsConfig == null) {
			throw new WebApplicationException(Response.serverError()
					.entity("no preferences registered for pid " + Constants.PREFERENCES_ID).build());
		}

		settings.forEach(idsConfig::put);
		
		try {
			idsConfig.flush();
			return "ok";
		} catch (BackingStoreException e) {
			throw new InternalServerErrorException(e);
		}		
	}

	/**
	 * Save connection configuration of a particular connection
	 * @param connection The name of the connection
	 * @param settings The connection configuration of the connection
	 * @return "ok" String
	 */
	@POST
	@Path("/connectionConfigs/{con}")
	@Consumes(MediaType.APPLICATION_JSON)
	public String setConnectionConfigurations(@PathParam("con") String connection, ConnectionSettings settings) {
		PreferencesService preferencesService = WebConsoleComponent.getPreferencesServiceOrThrowSUE();

		Preferences prefs = preferencesService.getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		
		if (settings == null) {
			throw new BadRequestException();
		}
		
		//Check connection's settings exist
		try {
			if(Arrays.stream(prefs.keys()).noneMatch(connection::equals)) {
				throw new NotFoundException();
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
			throw new InternalServerErrorException(e);
		}

		prefs.put(connection, new Gson().toJson(settings));
		return "ok";
	}
	
	/**
	 * Sends configuration of a connection
	 * @param connection Connection identifier
	 * @return The connection configuration of the requested connection
	 */
	@GET
	@Path("/connectionConfigs/{con}")
	@Produces(MediaType.APPLICATION_JSON)
	public ConnectionSettings getConnectionConfigurations(@PathParam("con") String connection) {
		PreferencesService preferencesService = WebConsoleComponent.getPreferencesServiceOrThrowSUE();

		Preferences preferences = preferencesService.getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		if (preferences == null) {
		    return new ConnectionSettings();
		} else {
			return new Gson().fromJson(preferences.get(connection, ""), ConnectionSettings.class);
		}
	}

	/**
	 * Sends configurations of all connections
	 * @return Map of connection names/configurations
	 */
	@GET
	@Path("/connectionConfigs")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, ConnectionSettings> getAllConnectionConfigurations() {
		PreferencesService preferencesService = WebConsoleComponent.getPreferencesServiceOrThrowSUE();
		ConnectionManager connectionManager = WebConsoleComponent.getConnectionManagerOrThrowSUE();
		RouteManager routeManager = WebConsoleComponent.getRouteManagerOrThrowSUE();

		Preferences prefs = preferencesService.getUserPreferences(Constants.CONNECTIONS_PREFERENCES);

		Map<String, List<String>> routeInputs = routeManager.getRoutes().stream().map(RouteObject::getId)
				.collect(Collectors.toMap(Function.identity(), routeManager::getRouteInputUris));

		for (IDSCPServerEndpoint endpoint: connectionManager.listAvailableEndpoints()) {
			try {					
				// For every currently available endpoint, go through all preferences and check
				// if the id is already there. If not, create empty config.
				String[] connections = prefs.keys();
				String hostIdentifier = endpoint.getHost() + ":" + endpoint.getPort();
				String serverUri = "idsserver://" + hostIdentifier;
				List<String> endpointIdentifiers = routeInputs.entrySet().stream()
						.filter(e -> e.getValue().stream().anyMatch(u -> u.startsWith(serverUri)))
						.map(e -> e.getKey() + " - " + hostIdentifier).collect(Collectors.toList());

				if (endpointIdentifiers.isEmpty()) {
					endpointIdentifiers = Collections.singletonList("<no route found>" + " - " + hostIdentifier);
				}

				// Create missing endpoint configurations
				endpointIdentifiers.forEach(endpointIdentifier -> {
					if (Arrays.stream(connections).noneMatch(endpointIdentifier::equals)) {
						prefs.put(endpointIdentifier, new Gson().toJson(new ConnectionSettings()));
					}
				});
			} catch (BackingStoreException e) {
				throw new InternalServerErrorException(e);
			}
		}
		
		//After synchronization, return complete set of preferences
		Map<String, ConnectionSettings> allSettings = new TreeMap<>((o1, o2) -> {
			if (ConfigApi.GENERAL_CONFIG.equals(o1)) {
				return -1;
			} else if (ConfigApi.GENERAL_CONFIG.equals(o2)) {
				return 1;
			} else {
				return o1.compareTo(o2);
			}
		});
		try {
			Gson gson = new Gson();
			Arrays.stream(prefs.keys()).forEach(connection -> allSettings.put(connection,
					gson.fromJson(prefs.get(connection, null), ConnectionSettings.class)));
		} catch (BackingStoreException e) {
			throw new InternalServerErrorException(e);
		}

		return allSettings;
	}

}
