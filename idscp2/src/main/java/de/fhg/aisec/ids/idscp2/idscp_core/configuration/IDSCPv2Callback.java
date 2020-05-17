package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpServerListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

/**
 * An callback interface that implements callback functions that notifies about new
 * secureChannels
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface IDSCPv2Callback {

    /*
     * Notify the client about a new secure channel
     */
    void secureChannelConnectHandler(SecureChannel secureChannel);

    /*
     * Notify the server about new secureChannel
     */
    void secureChannelListenHandler(
        SecureChannel secureChannel,
        IdscpServerListener idscpServer
    );

}