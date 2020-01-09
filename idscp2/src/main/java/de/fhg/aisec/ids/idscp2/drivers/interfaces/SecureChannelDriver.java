package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

/**
 * An interface for the IDSCPv2 SecureChannelDriver class, that implements a connect() function for IDSCPv2 clients and
 * a listen() function for IDSCPv2 servers
 *
 * Developer API
 *
 * Methods:
 * void connect(IDSCPv2Settings, IDSCPv2Callback)         to create an IDSCPv2 connection to the host from the settings
 *                                                        registers a callback for errors and return of a secure channel
 *
 * SecureServer listen(IDSCPv2Settings, IDSCPv2Callback)  to create an IDSCPv2 server configured by the IDSCPv2 settings
 *                                                        registers a callback for errors and new incoming connections
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureChannelDriver {
    void connect(IDSCPv2Settings settings, IDSCPv2Callback callback);
    SecureServer listen(IDSCPv2Settings settings, IDSCPv2Callback callback);
}
