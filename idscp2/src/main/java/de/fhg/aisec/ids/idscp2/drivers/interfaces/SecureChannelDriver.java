package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;

/**
 * An interface for the IDSCPv2 SecureChannelDriver class, that implements a connect() function
 * for IDSCPv2 clients and a listen() function for IDSCPv2 servers to connect the underlying layer
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureChannelDriver {

    /*
     * Asynchronous method to create a secure connection to a secure server
     */
    void connect(IDSCPv2Settings settings, IDSCPv2Callback configCallback);

    /*
     * Starting a secure server
     */
    SecureServer listen(IDSCPv2Settings settings, IDSCPv2Callback configCallback,
                        IdscpConnectionListener idscpServerCallback);
}
