package de.fhg.aisec.ids.api.endpointconfig;

/**
 * Interface of the Endpoint Configuration Listener.
 *
 * <p> The Endpoint Configuration Listener is part of a Listener Pattern for dynamically validate
 * the dynamicAttributeToken in the DefaultWebSocket when endpoint configuration has changed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface EndpointConfigListener {
    void updateTokenValidation();
}
