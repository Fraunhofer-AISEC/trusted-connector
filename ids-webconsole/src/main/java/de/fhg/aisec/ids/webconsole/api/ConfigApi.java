package de.fhg.aisec.ids.webconsole.api;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/config")
public class ConfigApi {
	
	@GET()
	@Path("/all")
	public Map<String, String> get() {
		return new HashMap<String, String>();
	}
}
