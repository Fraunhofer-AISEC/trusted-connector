package de.fhg.aisec.ids.webconsole.api;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.camel.CamelContext;
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

		// Visualize Camel routes in graphviz format
		// TODO Do not put all routes in one image but rather visualize each on its own.
		CamelRouteToDot viz = new CamelRouteToDot();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(bos);
		Map<String, List<RouteDefinition>> map = camelO.stream().collect(Collectors.toMap(c->c.getName(), c->c.getRouteDefinitions()));
		viz.generateFile(writer, map);
		writer.flush();
		String dot="";
		try {
			dot = bos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.error(e.getMessage(), e);
		}
		
		// Create response
		for (CamelContext cCtx : camelO) {
			for (RouteDefinition rd : cCtx.getRouteDefinitions()) {
				HashMap<String, String> route = new HashMap<>();				
				route.put("id", rd.getId());
				route.put("description", (rd.getDescriptionText()!=null)?rd.getDescriptionText():"");
				route.put("dot", dot);
				route.put("shortName", rd.getShortName());
				route.put("context", cCtx.getName());
				route.put("uptime", String.valueOf(cCtx.getUptimeMillis()));
				route.put("status", cCtx.getStatus().toString());
				result.add(route);
			}
		}		
		
		return new GsonBuilder().create().toJson(result);
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