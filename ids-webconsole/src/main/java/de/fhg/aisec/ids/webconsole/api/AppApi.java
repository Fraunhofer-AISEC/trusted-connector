/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.NoContainerExistsException;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fhg.aisec.ids.webconsole.api.data.AppSearchRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API interface for managing "apps" in the connector.
 *
 * <p>In this implementation, apps are either docker or trustX containers.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/apps/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Path("/app")
@Api(value = "App")
public class AppApi {
  public static final long PULL_TIMEOUT_MINUTES = 20;
  private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);

  @GET
  @Path("list")
  @ApiOperation(
    value = "List installed apps",
    notes = "Returns an empty list if no apps are installed",
    response = ApplicationContainer.class,
    responseContainer = "List"
  )
  @ApiResponses(@ApiResponse(code = 200, message = "List of apps"))
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationContainer> list() {
    ContainerManager cml = WebConsoleComponent.getContainerManager();

    List<ApplicationContainer> result = cml.list(false);
    result.sort(
        (app1, app2) -> {
          try {
            SimpleDateFormat d = new SimpleDateFormat("dd-MM-yyyy HH:mm:s Z");
            Date date1 = d.parse(app1.getCreated());
            Date date2 = d.parse(app2.getCreated());
            if (date1.getTime() < date2.getTime()) {
              return 1;
            } else {
              return -1;
            }
          } catch (Exception t) {
            LOG.warn("Unexpected app creation date/time. Cannot sort. {}", t.getMessage());
          }
          return 0;
        });
    return result;
  }

  @GET
  @Path("start/{containerId}")
  @ApiOperation(
      value = "Start an app",
      notes = "Requests to start an app.",
      response = Boolean.class
  )
  @ApiResponses(
      @ApiResponse(
          code = 200,
          message =
              "true if the app has been requested to be started. "
                  + "false if no container management layer is available"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  public boolean start(
      @ApiParam(value = "ID of the app to start") @PathParam("containerId") String containerId) {
    return start(containerId, null);
  }

  @GET
  @Path("start/{containerId}/{key}")
  @ApiOperation(
    value = "Start an app",
    notes = "Requests to start an app.",
    response = Boolean.class
  )
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message =
            "true if the app has been requested to be started. "
                + "false if no container management layer is available"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  public boolean start(
      @ApiParam(value = "ID of the app to start") @PathParam("containerId") String containerId,
      @ApiParam(value = "Key for user token") @PathParam("key") String key) {
    try {
      ContainerManager cml = WebConsoleComponent.getContainerManager();
      cml.startContainer(containerId, key);
      return true;
    } catch (NoContainerExistsException | ServiceUnavailableException e) {
      LOG.error("Error starting container", e);
      return false;
    }
  }

  @GET
  @Path("stop/{containerId}")
  @ApiOperation(value = "Stop an app", notes = "Requests to stop an app.", response = Boolean.class)
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message =
            "true if the app has been requested to be stopped. "
                + "false if no container management layer is available"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  public boolean stop(
      @ApiParam(value = "ID of the app to stop") @PathParam("containerId") String containerId) {
    try {
      ContainerManager cml = WebConsoleComponent.getContainerManager();
      cml.stopContainer(containerId);
      return true;
    } catch (NoContainerExistsException | ServiceUnavailableException e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  @POST
  @OPTIONS
  @Path("install")
  @ApiOperation(
    value = "Install an app",
    notes = "Requests to install an app.",
    response = Boolean.class
  )
  @ApiResponses({
    @ApiResponse(
      code = 200,
      message =
          "If the app has been requested to be installed. "
              + "The actual installation takes place asynchronously in the background "
              + "and will terminate after a timeout of 20 minutes",
      response = Boolean.class
    ),
    @ApiResponse(
      code = 500,
      message = "_No cmld_: If no container management layer is available",
      response = String.class
    ),
    @ApiResponse(
      code = 500,
      message = "_Null image_: If imageID not given",
      response = String.class
    )
  })
  @Produces(MediaType.APPLICATION_JSON)
  public String install(
      @ApiParam(value = "String with imageID", collectionFormat = "Map")
          Map<String, ApplicationContainer> apps) {
    ApplicationContainer app = apps.get("app");
    LOG.debug("Request to load {}", app.getImage());
    final ContainerManager cm = WebConsoleComponent.getContainerManager();

    final String image = app.getImage();
    if (image == null) {
      LOG.warn("Null image");
      throw new InternalServerErrorException("Null image");
    }
    LOG.debug("Pulling app {}", image);
    ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    // Pull image asynchronously and create new container
    final Future<String> handler =
        executor.submit(
            () -> {
              Optional<String> containerId = cm.pullImage(app);
              return containerId.orElse(null);
            });
    // Cancel pulling after 20 minutes, just in case.
    executor.schedule(() -> handler.cancel(true), PULL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    return "OK";
  }

  @GET
  @Path("wipe")
  @ApiOperation(value = "Wipes an app and all its data")
  @ApiResponses({
    @ApiResponse(code = 200, message = "If the app is being wiped"),
    @ApiResponse(code = 500, message = "_No cmld_ if no container management layer is available")
  })
  public String wipe(
      @ApiParam(value = "ID of the app to wipe") @QueryParam("containerId") String containerId) {
    try {
      ContainerManager cml = WebConsoleComponent.getContainerManager();
      cml.wipe(containerId);
    } catch (NoContainerExistsException e) {
      LOG.error(e.getMessage(), e);
    }
    return "OK";
  }

  @GET
  @Path("cml_version")
  @ApiOperation(
    value = "Returns the version of the currently active container management layer",
    response = Map.class
  )
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getCml() {
    try {
      ContainerManager cml = WebConsoleComponent.getContainerManager();
      Map<String, String> result = new HashMap<>();
      result.put("cml_version", cml.getVersion());
      return result;
    } catch (ServiceUnavailableException sue) {
      return Collections.emptyMap();
    }
  }

  @POST
  @Path("search")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationContainer> search(AppSearchRequest searchRequest) {
    String term = searchRequest.getSearchTerm();
    try {
      Client client = ClientBuilder.newBuilder().build();
      String url =
          WebConsoleComponent.getSettings().getConnectorConfig().getAppstoreUrl();

      String r = client.target(url).request(MediaType.TEXT_PLAIN).get(String.class);

      ObjectMapper mapper = new ObjectMapper();
      ApplicationContainer[] result = mapper.readValue(r, ApplicationContainer[].class);
      if (term != null && !term.equals("")) {
        return Arrays.asList(result)
            .parallelStream()
            .filter(
                app ->
                    (app.getName() != null && app.getName().contains(term))
                        || (app.getDescription() != null && app.getDescription().contains(term))
                        || (app.getImage() != null && app.getImage().contains(term))
                        || (app.getId() != null && app.getId().contains(term))
                        || (app.getCategories() != null && app.getCategories().contains(term)))
            .collect(Collectors.toList());
      } else {
        return Arrays.asList(result);
      }
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
