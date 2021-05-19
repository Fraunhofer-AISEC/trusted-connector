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
package de.fhg.aisec.ids.api.router

/**
 * Interface of internal routing manager inside the Core Platform.
 *
 *
 * The routing manager is responsible for forwarding messages between containers. One
 * implementation of this interface is based on Apache Camel, others might follow.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
interface RouteManager {
    /**
     * Returns a list of currently installed routes.
     *
     * @return All installed rules
     */
    val routes: List<RouteObject>

    /**
     * Returns a RouteObject for a given route ID.
     *
     * @param id The ID of the route to fetch
     * @return The queried route or null
     */
    fun getRoute(id: String): RouteObject?

    /**
     * Starts a route.
     *
     * @param routeId ID of the route to start
     * @throws RouteException If starting the route failed
     */
    @Throws(RouteException::class)
    fun startRoute(routeId: String)

    /**
     * Sends a request to stop a route. Camel will try to gracefully shut down the route and deliver
     * pending exchanges.
     *
     * @param routeId ID of the route to stop
     * @throws RouteException If stopping the route failed
     */
    @Throws(RouteException::class)
    fun stopRoute(routeId: String)

    /**
     * List all supported components, i.e. supported endpoint protocols.
     *
     * @return List of supported components
     */
    fun listComponents(): List<RouteComponent>

    /**
     * List all route endpoints, i.e. all URIs of routes existing in loaded Camel contexts
     *
     * @return Map of Camel context names to contained endpoint URIs
     */
    val endpoints: Map<String, Collection<String>>
    fun listEndpoints(): Map<String, String>

    /**
     * Save a route, replacing it with a new representation within the same context
     *
     * @param routeId ID of the route to save
     * @param routeRepresentation The new textual representation of the route (XML etc.)
     * @return The object representing the modified route
     * @throws RouteException If the route does not exist or some Exception was thrown during route
     * replacement.
     */
    @Throws(RouteException::class)
    fun saveRoute(routeId: String, routeRepresentation: String): RouteObject

    /**
     * Adds a route and starts it.
     *
     *
     * Endpoint declarations must be supported by the underlying implementation.
     *
     *
     * If the route id already exists, this method will throw a RouteException and not overwrite
     * the existing route.
     *
     * @param routeDefinition Textual representation of the route (XML etc.)
     * @throws RouteException if a route with the same id already exists or if any Exception is thrown
     * during loading and starting the route.
     */
    @Throws(RouteException::class)
    fun addRoute(routeDefinition: String)

    /**
     * Removes a route from one endpoint to another.
     *
     *
     * The deletion becomes immediately effective.
     *
     *
     * Endpoint declarations must be supported by the underlying implementation.
     *
     * @param routeId ID of the route to delete
     */
    fun delRoute(routeId: String)

    /**
     * Returns the given route in its original representation of the implementing engine.
     *
     *
     * Note that this method may return null if the implementing engine does not support a textual
     * route configuration.
     *
     *
     * For Apache Camel, this method will return the XML-based Camel DSL configuration file.
     *
     * @param routeId ID of the route to retrieve the String representation for
     * @return String representation of the route
     */
    fun getRouteAsString(routeId: String): String?

    /**
     * Returns a List of URIs of the given route's inputs (from definitions)
     *
     * @param routeId The identifier of the route
     * @return The from (input) URIs of the route
     */
    fun getRouteInputUris(routeId: String): List<String>

    /**
     * Returns aggregated runtime metrics of all installed routes.
     *
     * @return Map&lt;k,v&gt; where k is a string indicating the route id.
     */
    val routeMetrics: Map<String, RouteMetrics>

    /**
     * Returns the given route configuration in a Prolog representation.
     *
     * @param routeId ID of route to retrieve prolog representation for
     * @return Route represented as prolog
     */
    fun getRouteAsProlog(routeId: String): String
}
