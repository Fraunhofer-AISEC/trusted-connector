/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.api.router;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface of internal routing manager inside the Core Platform.
 *
 * <p>The routing manager is responsible for forwarding messages between containers. One
 * implementation of this interface is based on Apache Camel, others might follow.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public interface RouteManager {

  /**
   * Returns a list of currently installed routes.
   *
   * @return All installed rules
   */
  @NonNull
  List<RouteObject> getRoutes();

  /**
   * Returns a list of currently installed routes.
   *
   * @return The queried route or null
   */
  @Nullable
  RouteObject getRoute(@NonNull String id);

  /**
   * Starts a route.
   *
   * @param routeId
   */
  void startRoute(@NonNull String routeId) throws RouteException;

  /**
   * Sends a request to stop a route. Camel will try to gracefully shut down the route and deliver
   * pending exchanges.
   *
   * @param routeId
   * @throws Exception
   */
  void stopRoute(@NonNull String routeId) throws RouteException;

  /**
   * List all supported components, i.e. supported endpoint protocols.
   *
   * @return
   */
  @NonNull
  List<RouteComponent> listComponents();

  /**
   * List all route endpoints, i.e. all URLs to which routes exist.
   *
   * @return
   */
  @NonNull
  Map<String, Collection<String>> getEndpoints();

  @NonNull
  Map<String, String> listEndpoints();

  /**
   * Save a route, replacing it with a new representation within the same context
   *
   * @param routeId ID of the route to save
   * @param routeRepresentation The new textual representation of the route (XML etc.)
   * @return The object representing the modified route
   * @throws RouteException If the route does not exist or some Exception was thrown during route
   *     replacement.
   */
  @NonNull
  RouteObject saveRoute(@NonNull String routeId, @NonNull String routeRepresentation)
      throws RouteException;

  /**
   * Adds a route and starts it.
   *
   * <p>Endpoint declarations must be supported by the underlying implementation.
   *
   * <p>If the route id already exists, this method will throw a RouteException and not overwrite
   * the existing route.
   *
   * @param routeDefinition Textual representation of the route (XML etc.)
   * @throws RouteException if a route with the same id already exists or if any Exception is thrown
   *     during loading and starting the route.
   */
  void addRoute(@NonNull String routeDefinition) throws RouteException;

  /**
   * Removes a route from one endpoint to another.
   *
   * <p>The deletion becomes immediately effective.
   *
   * <p>Endpoint declarations must be supported by the underlying implementation.
   *
   * @param routeId
   */
  void delRoute(@NonNull String routeId);

  /**
   * Returns the given route in its original representation of the implementing engine.
   *
   * <p>Note that this method may return null if the implementing engine does not support a textual
   * route configuration.
   *
   * <p>For Apache Camel, this method will return the XML-based Camel DSL configuration file.
   *
   * @return String representation of the route
   */
  String getRouteAsString(@NonNull String routeId);

  /**
   * Returns a List of URIs of the given route's inputs (from definitions)
   *
   * @param routeId The identifier of the route
   * @return The from (input) URIs of the route
   */
  @NonNull
  List<String> getRouteInputUris(@NonNull String routeId);

  /**
   * Returns aggregated runtime metrics of all installed routes.
   *
   * @return map<k,v> where k is a string indicating the route id.
   */
  @NonNull
  Map<String, RouteMetrics> getRouteMetrics();

  /**
   * Returns the given route configuration in a Prolog representation.
   *
   * @param routeId ID of route to retrieve prolog representation for
   * @return Route represented as prolog
   */
  @NonNull
  String getRouteAsProlog(@NonNull String routeId);
}
