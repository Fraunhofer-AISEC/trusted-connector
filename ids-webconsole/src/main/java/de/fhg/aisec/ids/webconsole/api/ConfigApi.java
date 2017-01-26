package de.fhg.aisec.ids.webconsole.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

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
	private static final String IDS_CONFIG_SERVICE = "ids-webconsole";
	
	@GET()
	@Path("list")
	public String get() {
		Optional<ConfigurationAdmin> cO = WebConsoleComponent.getConfigService();
		
		// if config service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return new GsonBuilder().create().toJson(new HashMap<>());
		}
		
		try {
			return new GsonBuilder().create().toJson(cO.get().getConfiguration(IDS_CONFIG_SERVICE).getProperties());
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return "{}";
	}

	@POST
	@OPTIONS
	@Path("set")
	@Consumes("application/json")
	public String set(String settings) {
		LOG.info("Received string " + settings);
		Map<String, String> result = new GsonBuilder().create().fromJson(settings, new TypeToken<HashMap<String, String>>() {}.getType());
		Optional<ConfigurationAdmin> cO = WebConsoleComponent.getConfigService();
		
		// if config service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return "no config service";
		}

		try {
			Configuration idsConfig = cO.get().getConfiguration(IDS_CONFIG_SERVICE);
			if (idsConfig==null) {
				return "no config registered for pid " + IDS_CONFIG_SERVICE;
			}
			
			for (Iterator<?> iterator = result.keySet().iterator(); iterator.hasNext();) {
				Object key = iterator.next();
				Object value = result.get(key);
				idsConfig.getProperties().put(key.toString(), value);
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
		return "ok";
	}
}
