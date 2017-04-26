package de.fhg.aisec.ids.api.router;

import java.nio.ByteBuffer;
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


	public List<RouteObject> getRoutes();
	
	/**
	 * Starts a route.
	 * 
	 * @param routeId
	 */
	public void startRoute(String routeId) throws Exception;
	
	/**
	 * Sends a request to stop a route. Camel will try to gracefully shut down the route and deliver pending exchanges.
	 * 
	 * @param routeId
	 */
	public void stopRoute(String routeId) throws Exception;
	
	public List<RouteComponent> listComponents();
	
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
	String getRouteConfigAsString();
	
	void loadRoutes(String routeConfig);

	/**
	 * Creates a new endpoint at which messages are returned <i>out of</i> the routing engine.
	 * 
	 * Note the possible confusion: Producers retrieve messages from the outside and feed them into the engine.
	 * Consumers receive messages at the end of a route and forward them to some external endpoint.
	 * 
	 * This terminology might be confusing, but it is in line with Apache Camel.
	 * 
	 * @param ep
	 * @return true if a new endpoint has been successfully registered, false else (e.g., if that endpoint already exists).
	 */
	boolean createConsumingEndpoint(String ep);

	void removeConsumingEndpoint(String ep);


	public void provide(String ep, ByteBuffer msg);

}
