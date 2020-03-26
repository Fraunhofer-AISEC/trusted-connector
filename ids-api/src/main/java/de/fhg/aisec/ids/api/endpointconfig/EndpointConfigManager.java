package de.fhg.aisec.ids.api.endpointconfig;

/**
 * Interface of the Dynamic Endpoint Configuration Manager.
 *
 * <p> The Dynamic Endpoint Configuration Manager is part of a Listener Pattern for dynamic validation of
 * the dynamicAttributeToken in the DefaultWebSocket when endpoint configuration has changed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public interface EndpointConfigManager {

    void addEndpointConfigListener(String identifier, EndpointConfigListener listener);

    void removeEndpointConfigListener(String identifier);

    void notify(String endpointConfig);

}
