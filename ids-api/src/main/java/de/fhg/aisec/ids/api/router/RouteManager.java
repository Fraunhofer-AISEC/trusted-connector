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
	List<RouteObject> getRoutes();
	
	/**
	 * Starts a route.
	 * 
	 * @param routeId
	 */
	void startRoute(String routeId) throws Exception;
	
	/**
	 * Sends a request to stop a route. Camel will try to gracefully shut down the route and deliver pending exchanges.
	 * 
	 * @param routeId
	 * @throws Exception 
	 */
	void stopRoute(String routeId) throws RouteException;
	
	List<RouteComponent> listComponents();
	
	Map<String, Collection<String>> getEndpoints();
	
	Map<String,String> listEndpoints();
	
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
	 * @param from
	 * @param to
	 */
	void delRoute(String from, String to);

	/**
	 * Returns the current route configuration in its original representation of the implementing engine.
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
	 * Returns runtime metrics of all installed routes.
	 * 
	 * @return map<k,v> where k is a string indicating the route id. Never null.
	 */
	Map<String,RouteMetrics> getRouteMetrics() ;

	/**
	 * Returns runtime metrics of a single route or null if route does not exist.
	 * 
	 * @param routeId
	 * @return
	 */
	RouteMetrics getRouteMetrics(String routeId);
}
