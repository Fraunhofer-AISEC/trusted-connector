package de.fhg.aisec.ids.webconsole.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.camel.CamelContext;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for "data pipes" in the connector.
 * 
 * This implementation uses Camel Routes as data pipes, i.e. the API methods allow inspection of camel routes in different camel contexts.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/routes/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/routes")
public class RouteApi {
	
	/**
	 * Returns map from camel context to list of camel routes.
	 * 
	 * Example:
	 * 
	 * {"camel-1":["Route(demo-route)[[From[timer://simpleTimer?period\u003d10000]] -\u003e [SetBody[simple{This is a demo body!}], Log[The message contains ${body}]]]"]}
	 * 
	 * @return
	 */
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {				
		List<CamelContext> camelO = WebConsoleComponent.getCamelContexts();
		Map<String, List<String>> routes = camelO.stream()
				.collect(Collectors.toMap(c -> c.getName(), c -> c.getRouteDefinitions()
						.stream()
						.map(rd -> rd.toString())
						.collect(Collectors.toList())));		
		return new GsonBuilder().create().toJson(routes);
	}


	/**
	 * Returns map from camel contexts to list of camel components.
	 * 
	 * Example:
	 * 
	 * {"camel-1":["timer","properties"]}
	 * 
	 * @return
	 */
	@GET
	@Path("components")
	@Produces("application/json")
	public String getComponents() {				
		List<CamelContext> camelO = WebConsoleComponent.getCamelContexts();
		Map<String, List<String>> components = camelO.stream().collect(Collectors.toMap(c -> c.getName(), c -> c.getComponentNames()));
		return new GsonBuilder().create().toJson(components);
	}

	/**
	 * Returns map from camel contexts to list of endpoint URIs.
	 * 
	 * Example:
	 * 
	 * {"camel-1":["timer://simpleTimer?period\u003d10000"]}
	 * 
	 * @return
	 */
	@GET
	@Path("endpoints")
	@Produces("application/json")
	public String getEndpoints() {
		List<CamelContext> camelO = WebConsoleComponent.getCamelContexts();
		Map<String, Collection<String>> endpoints = camelO.stream().collect(Collectors.toMap(ctx -> ctx.getName(), ctx -> ctx.getEndpoints()
				.stream()
				.map(ep -> ep.getEndpointUri())
				.collect(Collectors.toList())));		
		return new GsonBuilder().create().toJson(endpoints);
	}
}