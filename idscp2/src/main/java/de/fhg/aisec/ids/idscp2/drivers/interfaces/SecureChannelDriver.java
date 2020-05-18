package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for the IDSCP2 SecureChannelDriver class, that implements a connect() function
 * for IDSCP2 clients and a listen() function for IDSCP2 servers to connect the underlying layer
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureChannelDriver {

    /*
     * Asynchronous method to create a secure connection to a secure server
     */
    void connect(Idscp2Settings settings, Idscp2Callback configCallback);

    /*
     * Starting a secure server
     */
    SecureServer listen(Idscp2Settings settings, Idscp2Callback configCallback,
                        CompletableFuture<Idscp2Connection> connectionPromise);
}
