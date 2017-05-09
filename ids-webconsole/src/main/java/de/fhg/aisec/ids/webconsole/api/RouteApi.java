package de.fhg.aisec.ids.webconsole.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteObject;
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
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return "{}";
		}
		
		return new GsonBuilder().create().toJson(rm.get().getRoutes());
	}

	@GET
	@Path("/get/{id}")
	@Produces("application/json")
	public String get(String id) {		
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return "{}";
		}
		Optional<RouteObject> oRoute = rm.get().getRoutes().stream().filter(r -> id.equals(r.getRouteId())).findAny();
		if (!oRoute.isPresent()) {
			return "{}";
		}
		return new GsonBuilder().create().toJson(oRoute.get());
	}

	/**
	 * Stop a route based on an id.
	 */
	@GET
	@Path("/startroute/{id}")
	public String startRoute(@PathParam("id") String id) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				rm.get().startRoute(id);
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				return "{\"status:\": \"error\"}";
			}
			return "{\"status\": \"ok\"}";	
		}
		return "{\"status:\": \"error\"}";
	}

	/**
	 * Stop a route based on its id.
	 */
	@GET
	@Path("/stoproute/{id}")
	public String stopRoute(@PathParam("id") String id) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				rm.get().stopRoute(id);
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				return "{\"status:\": \"error\"}";
			}
			return "{\"status\": \"ok\"}";	
		}
		return "{\"status:\": \"error\"}";
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
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return "{}";
		}
		return new GsonBuilder().create().toJson(rm.get().listComponents());
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
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return "{}";
		}
		return new GsonBuilder().create().toJson(rm.get().getEndpoints());
	}

	/**
	 * Retrieve list of supported components (aka protocols which can be addressed by Camel)
	 */
	@GET
	@Path("/list_components")
	public String listComponents() {
		List<Map<String, String>> componentNames = new ArrayList<>();
		BundleContext bCtx = FrameworkUtil.getBundle(WebConsoleComponent.class).getBundleContext();
		if (bCtx == null) {		
			return new GsonBuilder().create().toJson(componentNames);			
		}

		try {
			ServiceReference<?>[] services = bCtx.getServiceReferences("org.apache.camel.spi.ComponentResolver", null);
			for (ServiceReference<?> sr : services) {
				String bundle = sr.getBundle().getHeaders().get("Bundle-Name");
				if (bundle==null || "".equals(bundle)) {
					bundle = sr.getBundle().getSymbolicName();
				}
				String description = sr.getBundle().getHeaders().get("Bundle-Description");
				if (description==null) {
					description = "";
				}
				Map<String, String> component = new HashMap<>();
				component.put("name", bundle);
				component.put("description", description);
				componentNames.add(component);
			}
		} catch (InvalidSyntaxException e) {
			LOG.error(e.getMessage(), e);
		}
		return new GsonBuilder().create().toJson(componentNames);			
	}

	/**
	 * Retrieve list of currently installed endpoints (aka URIs to/from which routes exist)
	 */
	@GET
	@Path("/list_endpoints")
	public String listEndpoints() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return "{}";
		}
		return new GsonBuilder().create().toJson(rm.get().listEndpoints());
	}
}