package de.fhg.aisec.ids.webconsole.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/<method>.
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
		List<ApplicationContainer> result = new ArrayList<>();
		
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		if (cml.isPresent()) {
			result = cml.get().list(false);
		}
		
		return new GsonBuilder().create().toJson(result);
	}
	
	@POST
	@Path("pull")
	@Produces("application/json")
	public String pull(@QueryParam("imageId") String imageId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return new Gson().toJson("false");
		}

		new Thread() {
			@Override
			public void run() {
				cml.get().pullImage(imageId);		
			}
		}.start();			
		return new Gson().toJson("true");
	}
	
	@GET
	@Path("start")
	@Produces("application/json")
	public String start(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return new Gson().toJson("false");
		}

		cml.get().startContainer(containerId);
		return new Gson().toJson("true");
	}

	@GET
	@Path("stop")
	@Produces("application/json")
	public String stop(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		
		if (!cml.isPresent()) {
			return new Gson().toJson("false");
		}

		cml.get().stopContainer(containerId);
		return new Gson().toJson("true");
	}

	@GET
	@Path("wipe")
	@Produces("application/json")
	public String wipe(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return new Gson().toJson("false");
		}

		cml.get().wipe(containerId);
		return new Gson().toJson("true");
	}
}