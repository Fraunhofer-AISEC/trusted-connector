package de.fhg.aisec.ids.webconsole.api;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/config")
public class ConfigApi {
	
	@GET()
	@Path("list")
	public Map<String, String> get() {
		return null;
	}
}
