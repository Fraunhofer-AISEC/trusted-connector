package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

import java.util.concurrent.CompletableFuture;

/**
 * An callback interface that implements callback functions that notify about new
 * SecureChannels
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureChannelInitListener {

    /**
     * Notify the server about new secureChannel
     */
    void onSecureChannel(
            SecureChannel secureChannel,
            CompletableFuture<ServerConnectionListener> serverListenerPromise
    );

    void onError(Throwable t);

}