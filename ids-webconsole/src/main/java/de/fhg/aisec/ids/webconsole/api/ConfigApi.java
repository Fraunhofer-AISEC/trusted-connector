package de.fhg.aisec.ids.webconsole.api;

import java.util.HashMap;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.configuration.ConfigurationService;
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
	
	@GET()
	@Path("list")
	public String get() {
		Optional<ConfigurationService> cO = WebConsoleComponent.getConfigService();
		
		// if config service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return new GsonBuilder().create().toJson(new HashMap<>());
		}
		
		return new GsonBuilder().create().toJson(cO.get().list());
	}
}
