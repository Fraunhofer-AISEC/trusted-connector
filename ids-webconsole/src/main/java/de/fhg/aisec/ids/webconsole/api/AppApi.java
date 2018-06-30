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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.NoContainerExistsException;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.data.AppSearchRequest;

/**
 * REST API interface for managing "apps" in the connector.
 * 
 * In this implementation, apps are either docker or trustX containers.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/apps/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/app")
public class AppApi {
	private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);
	
	@GET
	@Path("list")
	@Produces("application/json")
	public List<ApplicationContainer> list() {
		List<ApplicationContainer> result = new ArrayList<>();

		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		if (cml.isPresent()) {
			result = cml.get().list(false);
		}

		result.sort((app1, app2) -> {
			try {
				SimpleDateFormat d = new SimpleDateFormat("dd-MM-yyyy HH:mm:s Z");
				Date date1 = d.parse(app1.getCreated());
				Date date2 = d.parse(app2.getCreated());
				if (date1.getTime() < date2.getTime()) {
					return 1;
				} else {
					return -1;
				}
			} catch (Throwable t) {
				LOG.warn("Unexpected app creation date/time. Cannot sort. " + t.getMessage());
			}
			return 0;
		});
		return result;
	}

	@GET
	@Path("start/{containerId}")
	@Produces("application/json")
	public boolean start(@PathParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().startContainer(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}

	@GET
	@Path("stop/{containerId}")
	@Produces("application/json")
	public boolean stop(@PathParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().stopContainer(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return true;
	}

	@POST
	@OPTIONS
	@Path("install")
	@Consumes("application/json")
	@Produces("application/json")
	public Response install(Map<String,ApplicationContainer> apps) {
		ApplicationContainer app = apps.get("app");
		LOG.debug("Request to load {}", app.getImage());
		final Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();
		if (!cml.isPresent()) {
			LOG.warn("No cmld");
			return Response.serverError().entity("No cmld").build();
		}

		final String image = app.getImage();
		if (image==null) {
			LOG.warn("Null image");
			return Response.serverError().entity("Null image").build();
		}
		LOG.debug("Pulling app {}", image);
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

		// Pull image asynchronously and create new container
		final Future<String> handler = executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				if (!cml.isPresent()) {
					return null;
				}
				Optional<String> containerId = cml.get().pullImage(app);
				return containerId.orElse(null);
			}
		});
		// Cancel pulling after 20 minutes, just in case.
		executor.schedule(() -> handler.cancel(true), 20, TimeUnit.MINUTES);
		return Response.ok().build();
	}

	@GET
	@Path("wipe")
	@Produces("application/json")
	public boolean wipe(@QueryParam("containerId") String containerId) {
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return false;
		}

		try {
			cml.get().wipe(containerId);
		} catch (NoContainerExistsException e) {
			LOG.error(e.getMessage(), e);
		}
		return false;
	}

	@GET
	@Path("cml_version")
	@Produces("application/json")
	public Map<String, String> getCml() {
		Map<String, String> result = new HashMap<>();
		Optional<ContainerManager> cml = WebConsoleComponent.getContainerManager();

		if (!cml.isPresent()) {
			return result;
		}

		result.put("cml_version", cml.get().getVersion());
		return result;
	}
	
	@POST
	@Path("search")
	@Consumes("application/json")
	@Produces("application/json")
	public Response search(AppSearchRequest searchRequest) throws KeyManagementException, NoSuchAlgorithmException {
		String term = searchRequest.getSearchTerm();
		try {
			    Client client = ClientBuilder.newBuilder().build();
			    String url = WebConsoleComponent.getSettingsOrThrowSUE().getConnectorConfig().getAppstoreUrl();

				WebTarget webTarget = client.target(url);
				Invocation.Builder invocationBuilder  = webTarget.request(MediaType.TEXT_PLAIN);
				Response response = invocationBuilder.get();
			    String r = response.readEntity(String.class);
				
				ObjectMapper mapper = new ObjectMapper();
				ApplicationContainer[] result = mapper.readValue(r, ApplicationContainer[].class);
				List<ApplicationContainer> apps;
				if (term!=null && !term.equals("")) {
					apps = Arrays
						.asList(result)
						.parallelStream()
						.filter(app -> 
							   (app.getName()!=null && app.getName().contains(term))
							|| (app.getDescription()!=null && app.getDescription().contains(term))
							|| (app.getImage()!=null && app.getImage().contains(term))
							|| (app.getId()!=null && app.getId().contains(term))
							|| (app.getCategories()!=null && app.getCategories().contains(term))
							)
						.collect(Collectors.toList());
				} else {
					apps = Arrays.asList(result);
				}
		    return Response.ok(apps.toArray(new ApplicationContainer[apps.size()])).build();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return Response.serverError().entity(e.getMessage()).build();
		}
	}
}