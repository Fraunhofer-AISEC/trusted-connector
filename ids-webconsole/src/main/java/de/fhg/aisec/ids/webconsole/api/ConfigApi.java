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

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.api.settings.ConnectionSettings;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API interface for configurations in the connector.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/config/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
@Path("/config")
@Api(value = "Config")
public class ConfigApi {
  public static final String GENERAL_CONFIG = "General Configuration";

  @GET
  @ApiOperation(value = "Retrieves the current configuration", response = ConnectorConfig.class)
  @Produces(MediaType.APPLICATION_JSON)
  public ConnectorConfig get() {
    Settings settings = WebConsoleComponent.getSettings();
    return settings.getConnectorConfig();
  }

  @POST
  @OPTIONS
  @ApiOperation(value = "Sets the configuration", response = ConnectorConfig.class)
  @ApiResponses(
      @ApiResponse(
        code = 500,
        message =
            "_No valid preferences received_: If incorrect configuration parameter is provided"
      ))
  @Consumes(MediaType.APPLICATION_JSON)
  public String set(ConnectorConfig config) {
    if (config == null) {
      throw new BadRequestException("No valid preferences received!");
    }

    Settings settings = WebConsoleComponent.getSettings();
    settings.setConnectorConfig(config);

    return "OK";
  }

  /**
   * Save connection configuration of a particular connection.
   *
   * @param connection The name of the connection
   * @param conSettings The connection configuration of the connection
   */
  @POST
  @Path("/connectionConfigs/{con}")
  @ApiOperation(value = "Save connection configuration of a particular connection")
  @ApiResponses(
      @ApiResponse(
        code = 500,
        message =
            "_No valid connection settings received!_: If incorrect connection settings parameter is provided"
      ))
  @Consumes(MediaType.APPLICATION_JSON)
  public Response setConnectionConfigurations(
      @PathParam("con") String connection, ConnectionSettings conSettings) {
    if (conSettings == null) {
      Response.serverError().entity("No valid connection settings received!").build();
    }

    Settings settings = WebConsoleComponent.getSettings();
    settings.setConnectionSettings(connection, conSettings);

    return Response.ok().build();
  }

  /**
   * Sends configuration of a connection
   *
   * @param connection Connection identifier
   * @return The connection configuration of the requested connection
   */
  @GET
  @Path("/connectionConfigs/{con}")
  @ApiOperation(value = "Sends configuration of a connection", response = ConnectionSettings.class)
  @Produces(MediaType.APPLICATION_JSON)
  public ConnectionSettings getConnectionConfigurations(@PathParam("con") String connection) {
    Settings settings = WebConsoleComponent.getSettings();
    return settings.getConnectionSettings(connection);
  }

  /**
   * Sends configurations of all connections
   *
   * @return Map of connection names/configurations
   */
  @GET
  @Path("/connectionConfigs")
  @ApiOperation(value = "Retrieves configurations of all connections")
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message = "Map of connections and configurations",
        response = ConnectionSettings.class,
        responseContainer = "Map"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, ConnectionSettings> getAllConnectionConfigurations() {
    Settings settings = WebConsoleComponent.getSettings();
    ConnectionManager connectionManager = WebConsoleComponent.getConnectionManager();
    RouteManager routeManager = WebConsoleComponent.getRouteManagerOrThrowSUE();

    // Set of all connection configurations, properly ordered
    Map<String, ConnectionSettings> allSettings =
        new TreeMap<>(
            (o1, o2) -> {
              if (ConfigApi.GENERAL_CONFIG.equals(o1)) {
                return -1;
              } else if (ConfigApi.GENERAL_CONFIG.equals(o2)) {
                return 1;
              } else {
                return o1.compareTo(o2);
              }
            });
    // Load all existing entries
    allSettings.putAll(settings.getAllConnectionSettings());
    // Assert global configuration entry
    allSettings.putIfAbsent(ConfigApi.GENERAL_CONFIG, new ConnectionSettings());

    Map<String, List<String>> routeInputs =
        routeManager
            .getRoutes()
            .stream()
            .map(RouteObject::getId)
            .collect(Collectors.toMap(Function.identity(), routeManager::getRouteInputUris));

    for (IDSCPServerEndpoint endpoint : connectionManager.listAvailableEndpoints()) {
      // For every currently available endpoint, go through all preferences and check
      // if the id is already there. If not, create empty config.
      String hostIdentifier = endpoint.getHost() + ":" + endpoint.getPort();
      String serverUri = "idsserver://" + hostIdentifier;
      List<String> endpointIdentifiers =
          routeInputs
              .entrySet()
              .stream()
              .filter(e -> e.getValue().stream().anyMatch(u -> u.startsWith(serverUri)))
              .map(e -> e.getKey() + " - " + hostIdentifier)
              .collect(Collectors.toList());

      if (endpointIdentifiers.isEmpty()) {
        endpointIdentifiers =
            Collections.singletonList("<no route found>" + " - " + hostIdentifier);
      }

      // Create missing endpoint configurations
      endpointIdentifiers.forEach(
          endpointIdentifier -> {
            if (allSettings.keySet().stream().noneMatch(endpointIdentifier::equals)) {
              allSettings.put(endpointIdentifier, new ConnectionSettings());
            }
          });
    }

    return allSettings;
  }
}
