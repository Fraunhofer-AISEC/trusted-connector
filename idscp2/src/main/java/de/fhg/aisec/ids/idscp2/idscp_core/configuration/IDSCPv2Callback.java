package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

/**
 * An callback interface, that implements callback functions that notifies about new secureChannels, errors and closed
 * connections
 *
 * Developer API
 *
 * Methods:
 * void secureChannelConnectHandler(SecureChannel)      to notify the client about a new secure channel
 * void secureChannelListenHandler(SecureChannel)       to notify the server about a new secure channel
 * void connectionClosedHandler(String connectionId)    to notify listener about connection was closed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface IDSCPv2Callback {
    void secureChannelConnectHandler(SecureChannel secureChannel);
    void secureChannelListenHandler(SecureChannel secureChannel);
    void connectionClosedHandler(String connectionId);
}
