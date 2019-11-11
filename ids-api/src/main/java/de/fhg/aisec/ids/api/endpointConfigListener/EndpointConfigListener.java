package de.fhg.aisec.ids.api.endpointConfigListener;

/**
 * Interface of the Endpoint Configuration Listener.
 *
 * <p> The Endpoint Configuration Listener is part of an observer pattern, that 
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface EndpointConfigListener {
    void updateTokenValidation(String endpointConfig);
}
