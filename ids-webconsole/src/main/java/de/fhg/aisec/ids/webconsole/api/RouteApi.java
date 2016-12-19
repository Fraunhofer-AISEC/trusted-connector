package de.fhg.aisec.ids.webconsole.api;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.helper.CamelRouteToDot;

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
	private static final Logger LOG = LoggerFactory.getLogger(RouteApi.class);
	
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
		List<HashMap<String, String>> result = new ArrayList<>();
		List<CamelContext> camelO = WebConsoleComponent.getCamelContexts();

		// Create response
		for (CamelContext cCtx : camelO) {
			for (RouteDefinition rd : cCtx.getRouteDefinitions()) {
				HashMap<String, String> route = new HashMap<>();				
				route.put("id", rd.getId());
				route.put("description", (rd.getDescriptionText()!=null)?rd.getDescriptionText():"");
				route.put("dot", routeToDot(rd)); // Visualize route in graphviz
				route.put("shortName", rd.getShortName());
				route.put("context", cCtx.getName());
				route.put("uptime", String.valueOf(cCtx.getUptimeMillis()));
				route.put("uptime", String.valueOf(cCtx.getUptimeMillis()));
				route.put("status",cCtx.getRouteStatus(rd.getId()).toString());
				result.add(route);
			}
		}	
		
		
		return new GsonBuilder().create().toJson(result);
	}


	/**
	 * Stop a route based on an id.
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	@GET
	@Path("/stoproute/{id}")
	public String stopRoute(@PathParam("id") String id) {
		List<CamelContext> camelO = WebConsoleComponent.getCamelContexts();
		
		for (CamelContext cCtx : camelO) {
			Route rt = cCtx.getRoute(id);
			if(rt != null)
			{
				try {
					cCtx.suspendRoute(id);//stopRoute(id);
				} catch(Exception e) {
					return "{\"status\": \"bad\"}";
				}
			}
		}
		
		return "{\"status\": \"ok\"}";	
		
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
	

	private String routeToDot(RouteDefinition rd) {
		String result="";
		try {
			CamelRouteToDot viz = new CamelRouteToDot();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bos, "UTF-8"));
			viz.printSingleRoute(writer, rd);
			writer.flush();
			result = bos.toString("UTF-8");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}
}