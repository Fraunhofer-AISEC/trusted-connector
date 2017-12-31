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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.RouteDefinition;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.fhg.aisec.ids.api.Constants;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.connectionsettings.*;


/**
 * REST API interface for configurations in the connector.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/config/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
@Path("/config")
public class ConfigApi {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigApi.class);
	
	@GET()
	@Path("list")
	public Map<String,String> get() {
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		
		// if config service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return new HashMap<>();
		}
		
		Preferences prefs = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		if (prefs == null) {
			return new HashMap<>();
		}

		
		HashMap<String, String> pMap = new HashMap<>();
		try {
			for (String key : prefs.keys()) {
				pMap.put(key, prefs.get(key,null));
			}
		} catch (BackingStoreException e) {
			LOG.error(e.getMessage(), e);
		}
		return pMap;
	}

	@POST
	@OPTIONS
	@Path("set")
	@Consumes("application/json")
	public Response set(Map<String,String> settings) {
		Optional<PreferencesService> cO = WebConsoleComponent.getConfigService();
		
		if (settings==null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		// if preferences service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return Response.serverError().encoding("no preferences service").build();
		}
		
		// Store into preferences service
		Preferences idsConfig = cO.get().getUserPreferences(Constants.PREFERENCES_ID);
		if (idsConfig==null) {
			return Response.serverError().entity("no preferences registered for pid " + Constants.PREFERENCES_ID).build();
		}
		
		for (Iterator<String> iterator = settings.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String value = settings.get(key);
			idsConfig.put(key, value);
		}
		
		try {
			idsConfig.flush();
			return Response.ok("ok").build();
		} catch (BackingStoreException e) {
			LOG.error(e.getMessage(), e);
			return Response.serverError().entity(e.getMessage()).build();
		}		
	}
	
	/***
	 * 
	 * @param settings
	 * @return
	 */
	@POST
	@Path("setconnectionconfigs")
	@Consumes("application/json")
	public Response setConnectionConfigurations(@QueryParam("connection") String connection,
												Map<String,String> settings) {
		Optional<PreferencesService> confService = WebConsoleComponent.getConfigService();
		Preferences prefs = confService.get().getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		
		if (settings == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		//Check connection's settings exist
		try {
			String[] connections = prefs.keys();
			boolean connExist = false;
			for (int _i = 0; _i < connections.length; _i++) {
				if(connections[_i].equals(connection)) {
					connExist = true;
					break;
				}
				
			}
			if(!connExist) {
				return Response.status(500).entity("no such node").build();
			}
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JsonObject sets = new JsonObject();
		Gson gson = new Gson();
		
		for (Iterator<String> iterator = settings.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String value = settings.get(key);
			switch (key) {
			case "integrityProtectionandVerification" :
			case "authentication":
			case "serviceIsolation":
			case "integrityProtectionVerificationScope":
			case "appExecutionResources":
			case "dataUsageControlSupport":
			case "auditLogging":
			case "localDataConfidentiality":
				sets.addProperty(key, value);
				break;

			default:
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		}
		
		
		
		prefs.put(connection, gson.toJson(sets));
		prefs.get(connection, "");
		
	    return Response.status(200).entity("ok").build();
	}
	
	/***
	 * Sends back configuration of a connection
	 * @param connection
	 * @return
	 */
	@GET()
	@Path("/getconnectionconfigs")
	@Produces("application/json")
	public String getConnectionConfigurations(@QueryParam("connection") String connection) {
		
		Gson gson = new Gson();
		JsonObject config = new JsonObject();
		
		Optional<PreferencesService> confService = WebConsoleComponent.getConfigService();
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		Preferences prefs = confService.get().getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		
		if(prefs == null) {
			config.addProperty(connection,"");
		    return gson.toJson(config);
		}
	    
		JsonParser parser=new JsonParser();
		config.add(connection,parser.parse(prefs.get(connection, "")));
		
	    return gson.toJson(config);
	}

	/***
	 * 
	 * @return
	 */
	
	@GET()
	@Path("/getallconnectionconfigs")
	@Produces("application/json")
	public List<ConnectionSetting> getAll() {
		List<ConnectionSetting> allSettings = new ArrayList<>();
		Optional<PreferencesService> confService = WebConsoleComponent.getConfigService();
		Preferences prefs = confService.get().getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		Optional<RouteManager> routeManager = WebConsoleComponent.getRouteManager();
		
		List<IDSCPServerEndpoint> endpoints = connectionManager.get().listAvailableEndpoints();
		Iterator<IDSCPServerEndpoint> endpointIterator = endpoints.iterator();
		boolean found = false;
		
		List<RouteObject> routeObjects = routeManager.get().getRoutes();
		
		while(endpointIterator.hasNext()) {
			IDSCPServerEndpoint endpoint = endpointIterator.next();
			try {					
				//For every currently available endpoint, go through all preferences and check if the id is already there. If not, create emtpy config
				String[] connections = prefs.keys();
				String hostIdentifier = endpoint.getHost() + ":" + endpoint.getPort();
				String endpointIdentifier = "noRouteFound" + "-" + hostIdentifier;
    			for(RouteObject route: routeObjects) {
					String label = route.getTxtRepresentation();
					if(label != null  && label.contains("idsserver") && label.contains(hostIdentifier)) {
						endpointIdentifier = route.getId() + "-"+ hostIdentifier;
					} 

    			}
    			
				for (int i = 0; i < connections.length; i++) {
					if(connections[i].equals(endpointIdentifier)) {
						found = true;
					}
				}
				//if not already present, create
				if(!found) {
					prefs.put(endpointIdentifier, Helper.createDefaultJsonConfig());
				}
				
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//After synch, get complet set of prefs. 
		prefs = confService.get().getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		try {
			String[] connections = prefs.keys();
			for (int i = 0; i < connections.length; i++) {
				Settings sets = Helper.convertToSettings(prefs.get(connections[i], ""));
				ConnectionSetting cs = new ConnectionSetting(connections[i], sets);
				allSettings.add(cs);
				
			}
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return allSettings;
	}
	
	/***
	 * 
	 * @param connection
	 * @return
	 */
	@GET()
	@Path("/setsampleconnectionconfigs")
	public String setSampleConnectionConfigurations(@QueryParam("connection") String connection) {
		
		//ConnectionPreferenceManager.addConnectionPreferences(target);
		//Gson g = new GsonBuilder().disableHtmlEscaping().create();
		//return g.toJson(ConnectionPreferenceManager.findConnectionPreferences(target));
		
		Optional<PreferencesService> confService = WebConsoleComponent.getConfigService();
		Preferences prefs = confService.get().getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		
		prefs.put(connection, Helper.createDefaultJsonConfig());
		return prefs.get(connection, "");
	}

}
