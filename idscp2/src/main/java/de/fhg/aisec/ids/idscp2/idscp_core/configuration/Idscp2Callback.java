package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

import java.util.concurrent.CompletableFuture;

/**
 * An callback interface that implements callback functions that notify about new
 * SecureChannels
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface Idscp2Callback {

    /*
     * Notify the client about a new secure channel
     */
    void secureChannelConnectHandler(SecureChannel secureChannel);

    /*
     * Notify the server about new secureChannel
     */
    void secureChannelListenHandler(
        SecureChannel secureChannel,
        CompletableFuture<Idscp2Connection> connectionPromise
    );

}