package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

/**
 * An callback interface, that implements callback functions that notifies about new secureChannels, errors and closed
 * connections
 *
 * Developer API
 *
 * Methods:
 * void secureChannelConnectHandler(SecureChannel)      to notify the client about a new secure channel
 * void secureChannelListenHandler(SecureChannel, IdscpConnectionListener) to notify the server about new secureChannel
 * void connectionClosedHandler(String connectionId)    to notify listener about connection was closed
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface IDSCPv2Callback {
    void secureChannelConnectHandler(SecureChannel secureChannel);
    void secureChannelListenHandler(SecureChannel secureChannel, IdscpConnectionListener idscpServer);
    void connectionClosedHandler(String connectionId);
}
