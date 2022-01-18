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
     * Returns a List of URIs of the given route's inputs (from definitions)
     *
     * @param routeId The identifier of the route
     * @return The from (input) URIs of the route
     */
    fun getRouteInputUris(routeId: String): List<String>

    /**
     * Returns the given route configuration in a Prolog representation.
     *
     * @param routeId ID of route to retrieve prolog representation for
     * @return Route represented as prolog
     */
    fun getRouteAsProlog(routeId: String): String
}
