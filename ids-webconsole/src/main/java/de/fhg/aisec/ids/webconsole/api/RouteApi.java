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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fhg.aisec.ids.api.RouteResult;
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

	@GET
	@Path("/getAsString/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getAsString(@PathParam("id") String id) {
		Optional<RouteManager> rmOpt = WebConsoleComponent.getRouteManager();
		if (!rmOpt.isPresent()) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("RouteManager not present").build();
		}
		String routeAsString = rmOpt.get().getRouteAsString(id);
		if (routeAsString == null) {
			return Response.status(Response.Status.NOT_FOUND).entity("Route not found").build();
		}
		return Response.ok(routeAsString).build();
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
	@Path("/save/{id}")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public RouteResult saveRoute(@PathParam("id") String id, String routeDefinition) {
		RouteResult result = new RouteResult();
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			result.setMessage("No Route Manager present!");
			result.setSuccessful(false);
			return result;
		}
		try {
			result.setRoute(rm.get().saveRoute(id, routeDefinition));
		} catch (Exception e) {
			LOG.warn(e.getMessage(), e);
			result.setSuccessful(false);
			result.setMessage(e.getMessage());
		}
		return result;
	}

	@PUT
	@Path("/add")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public Result addRoute(String routeDefinition) {
		Result result = new Result();
		Optional<RouteManager> rm = WebConsoleComponent.getRouteManager();
		if (!rm.isPresent()) {
			result.setMessage("No Route Manager present!");
			result.setSuccessful(false);
			return result;
		}
		try {
			rm.get().addRoute(routeDefinition);
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
	 * Retrieve list of supported components (aka protocols which can be addressed by Camel)
	 *
	 * @return List of supported protocols
	 */
	@GET
	@Path("/components")
	@Produces("application/json")
	public List<RouteComponent> getComponents() {
		RouteManager rm = WebConsoleComponent.getRouteManagerOrThrowSUE();
		return rm.listComponents();
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