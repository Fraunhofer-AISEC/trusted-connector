package de.fhg.aisec.ids.webconsole.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/routes")
public class RouteApi {
	
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {
				
		Optional<OsgiDefaultCamelContext> camelO = WebConsoleComponent.getCamelContext();
		if (!camelO.isPresent()) {
			return new Gson().toJson("false");
		}
					
		OsgiDefaultCamelContext camel = camelO.get();
		List<Route> routes = camel.getRoutes();
		return new GsonBuilder().create().toJson(routes);
	}


	@GET
	@Path("components")
	@Produces("application/json")
	public String getComponents() {
				
		Optional<OsgiDefaultCamelContext> camelO = WebConsoleComponent.getCamelContext();
		if (!camelO.isPresent()) {
			return new Gson().toJson(new String[0]);
		}
					
		OsgiDefaultCamelContext camel = camelO.get();
		List<String> components = camel.getComponentNames();
		return new GsonBuilder().create().toJson(components);
	}

	@GET
	@Path("endpoints")
	@Produces("application/json")
	public String getEndpoints() {
				
		Optional<OsgiDefaultCamelContext> camelO = WebConsoleComponent.getCamelContext();
		if (!camelO.isPresent()) {
			return new Gson().toJson(new String[0]);
		}
					
		OsgiDefaultCamelContext camel = camelO.get();
		Collection<Endpoint> endpoints = camel.getEndpoints();
		List<String> epURIs = endpoints.stream().map(ep -> ep.getEndpointUri()).collect(Collectors.toList());
		return new GsonBuilder().create().toJson(epURIs);
	}
}