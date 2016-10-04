package de.fhg.aisec.ids.webconsole.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * REST API interface.
 * 
 * The API will be available at http://localhost:8181/ids/api/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunofer.de)
 *
 */
@Path("/")
public interface Api {

	@GET
    @Path("config")
	@Produces( "application/json" )
	public String getConfig();

	@GET
	@Path("apps")
	@Produces("application/json")
	String listContainers();
}