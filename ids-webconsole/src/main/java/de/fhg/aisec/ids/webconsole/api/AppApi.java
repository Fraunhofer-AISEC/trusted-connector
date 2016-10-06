package de.fhg.aisec.ids.webconsole.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.google.gson.Gson;

import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface.
 * 
 * The API will be available at http://localhost:8181/cxf/api/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/apps")
public class AppApi {
	
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		if (cml==null) {
			return new Gson().toJson("false");
		}
		
		return new Gson().toJson(cml.list(true));		
	}
	
	@GET
	@Path("pull")
	@Produces("application/json")
	public String pull(@QueryParam("imageId") String imageId) {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.pullImage(imageId);
		return new Gson().toJson("OK");
	}
	
	@GET
	@Path("start")
	@Produces("application/json")
	public String start(@QueryParam("containerId") String containerId) {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.startContainer(containerId);
		return new Gson().toJson("OK");
	}

	@GET
	@Path("stop")
	@Produces("application/json")
	public String stop(@QueryParam("containerId") String containerId) {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.stopContainer(containerId);
		return new Gson().toJson("OK");
	}

	@GET
	@Path("wipe")
	@Produces("application/json")
	public String wipe(@QueryParam("containerId") String containerId) {
		ContainerManager cml = WebConsoleComponent.getContainerManager();
		cml.wipe(containerId);
		return new Gson().toJson("OK");
	}
}