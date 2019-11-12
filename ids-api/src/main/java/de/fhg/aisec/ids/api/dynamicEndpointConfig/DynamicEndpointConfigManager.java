package de.fhg.aisec.ids.api.dynamicEndpointConfig;

/**
 * Interface of the Dynamic Endpoint Configuration Manager.
 *
 * <p> The Dynamic Endpoint Configuration Manager is part of a Listener Pattern for dynamically validate
 * the dynamicAttributeToken in the DefaultWebSocket when endpoint configuration has changed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public interface DynamicEndpointConfigManager {

    void notifyAll(String endpointConfig);

}
