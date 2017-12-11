/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform API
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
package de.fhg.aisec.ids.api.router;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Interface of internal routing manager inside the Core Platform.
 * 
 * The routing manager is responsible for forwarding messages between containers.
 * One implementation of this interface is based on Apache Camel, others might follow.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface RouteManager {

	/**
	 * Returns a list of currently installed routes.
	 * 
	 * @return
	 */
	public List<RouteObject> getRoutes();
	
	/**
	 * Starts a route.
	 * 
	 * @param routeId
	 */
	public void startRoute(String routeId) throws RouteException;
	
	/**
	 * Sends a request to stop a route. Camel will try to gracefully shut down the route and deliver pending exchanges.
	 * 
	 * @param routeId
	 * @throws Exception 
	 */
	public void stopRoute(String routeId) throws RouteException;
	
	/**
	 * List all supported components, i.e. supported endpoint protocols.
	 * 
	 * @return
	 */
	public List<RouteComponent> listComponents();
	
	/**
	 * List all route endpoints, i.e. all URLs to which routes exist.
	 * 
	 * @return
	 */
	public Map<String, Collection<String>> getEndpoints();
	
	public Map<String,String> listEndpoints();
	
	/**
	 * Adds a route from one endpoint to another. 
	 * 
	 * The route only becomes immediately effective.
	 * 
	 * Endpoint declarations must be supported by the underlying implementation.
	 * 
	 * @param from
	 * @param to
	 */
	void addRoute(String from, String to);

	
	/**
	 * Removes a route from one endpoint to another.
	 * 
	 * The deletion becomes immediately effective.
	 * 
	 * Endpoint declarations must be supported by the underlying implementation.
	 * 
	 * @param routeId
	 */
	void delRoute(String routeId);

	/**
	 * Returns the given route in its original representation of the implementing engine.
	 * 
	 * Note that this method may return null if the implementing engine does not support a textual route configuration.
	 * 
	 * For Apache Camel, this method will return the XML-based Camel DSL configuration file.
	 * 
	 * @return
	 */
	String getRouteAsString(String routeId);
	
	void loadRoutes(String routeConfig);
	
	/**
	 * Returns aggregated runtime metrics of all installed routes.
	 * 
	 * @return map<k,v> where k is a string indicating the route id.
	 */
	Map<String,RouteMetrics> getRouteMetrics() ;

	/**
	 * Returns the given route configuration in a Prolog representation.
	 * 
	 * @param routeId
	 * @return
	 */
	String getRouteAsProlog(String routeId);
}
