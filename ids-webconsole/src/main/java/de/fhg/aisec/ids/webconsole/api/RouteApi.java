/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.webconsole.api;

import java.util.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.Result;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.router.RouteComponent;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteMetrics;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.api.router.RouteVerificationProof;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.data.ValidationInfo;

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
	 * @return The resulting route objects
	 */
	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<RouteObject> list() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return Collections.emptyList();
		}
		return rm.get().getRoutes();
	}

	@GET
	@Path("/get/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@PathParam("id") String id) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("RouteManager not present").build();
		}
		RouteObject oRoute = rm.get().getRoute(id);
		if (oRoute == null) {
			return Response.status(Response.Status.NOT_FOUND).entity("Route not found").build();
		}
		return Response.ok(oRoute).build();
	}

	/**
	 * Stop a route based on an id.
	 */
	@GET
	@Path("/startroute/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result startRoute(@PathParam("id") String id) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				rm.get().startRoute(id);
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				return new Result(false, e.getMessage());
			}
			return new Result();
		}
		return new Result();
	}

	@POST
	@Path("save")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Result saveRoute(RouteObject route) {
		Result result = new Result();
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			result.setMessage("no route manager");
			result.setSuccessful(false);
			return result;
		}

		try {
			rm.get().addRoute(route);
		} catch (Exception e) {
			LOG.warn(e.getMessage(), e);
			result.setSuccessful(false);
			result.setMessage(e.getMessage());
		}
		return result;
	}

	/**
	 * Stop a route based on its id.
	 */
	@GET
	@Path("/stoproute/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopRoute(@PathParam("id") String id) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				rm.get().stopRoute(id);
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				return new Result(false, e.getMessage());
			}
			return new Result();
		}
		return new Result(false, "No route manager");
	}

	/**
	 * Get runtime metrics of a route
	 */
	@GET
	@Path("/metrics/{id}")
	public RouteMetrics getMetrics(@PathParam("id") String routeId) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				return rm.get().getRouteMetrics().get(routeId);
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
				return null;
			}
		}
		return null;
	}

	/**
	 * Get aggregated runtime metrics of all routes
	 */
	@GET
	@Path("/metrics")
	public RouteMetrics getMetrics() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (rm.isPresent()) {
			try {
				Map<String, RouteMetrics> currentMetrics = rm.get().getRouteMetrics();
				return aggregateMetrics(currentMetrics.values());
			} catch (Exception e) {
				LOG.debug(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Aggregates metrics of several rules
	 * 
	 * @param currentMetrics
	 * @return
	 */
	private RouteMetrics aggregateMetrics(Collection<RouteMetrics> currentMetrics) {
		RouteMetrics metrics = new RouteMetrics();
		currentMetrics.parallelStream().forEach(m -> {
			metrics.setCompleted(metrics.getCompleted() + m.getCompleted());
			metrics.setFailed(metrics.getFailed() + m.getFailed());
			metrics.setFailuresHandled(metrics.getFailuresHandled() + m.getFailuresHandled());
			metrics.setInflight(metrics.getInflight() + m.getInflight());
			metrics.setMaxProcessingTime(Math.max(metrics.getMaxProcessingTime(), m.getMaxProcessingTime()));
			metrics.setMeanProcessingTime(metrics.getMeanProcessingTime() + m.getMeanProcessingTime());
			metrics.setMinProcessingTime(Math.min(metrics.getMinProcessingTime(), m.getMinProcessingTime()));
			metrics.setCompleted(metrics.getCompleted() + m.getCompleted());
		});
		int metricsCount = currentMetrics.size();
		metrics.setMeanProcessingTime(metrics.getMeanProcessingTime()/(metricsCount!=0?metricsCount:1));
		return metrics;
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
	public List<RouteComponent> getComponents() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return new ArrayList<>();
		}
		return rm.get().listComponents();
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
	public Map<String, Collection<String>> getEndpoints() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return new HashMap<>();
		}
		return rm.get().getEndpoints();
	}

	/**
	 * Retrieve list of supported components (aka protocols which can be addressed by Camel)
	 */
	@GET
	@Path("/list_components")
	public List<Map<String, String>> listComponents() {
		List<Map<String, String>> componentNames = new ArrayList<>();
		BundleContext bCtx = FrameworkUtil.getBundle(WebConsoleComponent.class).getBundleContext();
		if (bCtx == null) {		
			return componentNames;			
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
		return componentNames;			
	}

	/**
	 * Retrieve list of currently installed endpoints (aka URIs to/from which routes exist)
	 */
	@GET
	@Path("/list_endpoints")
	public Map<String, String> listEndpoints() {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return new HashMap<>();
		}
		return rm.get().listEndpoints();
	}

	@GET
	@Path("/validate/{routeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response validate(@PathParam("routeId") String routeId) {
		Optional<PAP> pap = WebConsoleComponent.getPolicyAdministrationPoint();
		if (!pap.isPresent()) {
			return Response.serverError().entity("PolicyAdministrationPoint not available").build();
		}
		RouteVerificationProof rvp = pap.get().verifyRoute(routeId);
		ValidationInfo vi = new ValidationInfo();
		vi.valid = rvp.isValid();
		if (!rvp.isValid()) {
			vi.counterExamples = rvp.getCounterExamples();
		}
		return Response.ok(vi).build();
	}

	@GET
	@Path("/prolog/{routeId}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getRouteProlog(@PathParam("routeId") String routeId) {
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			return Response.serverError().entity("RouteManager not available").build();
		}
		return Response.ok(rm.get().getRouteAsProlog(routeId)).build();
	}
}