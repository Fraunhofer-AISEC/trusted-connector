package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client.TLSClient;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server.TLSServer;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.SecureChannelInitListener;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of SecureChannelDriver interface on TLSv1.3
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class NativeTLSDriver implements SecureChannelDriver {
    private static final Logger LOG = LoggerFactory.getLogger(NativeTLSDriver.class);

    /**
     * Performs an asynchronous client connect to a TLS server.
     */
    @Override
    public void connect(Idscp2Settings settings,
                        DapsDriver dapsDriver,
                        CompletableFuture<Idscp2Connection> connectionFuture) {
        try {
            TLSClient tlsClient = new TLSClient(settings, dapsDriver, connectionFuture);
            tlsClient.connect(settings.getHost(), settings.getServerPort());
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            connectionFuture.completeExceptionally(new Idscp2Exception("Call to connect() has failed", e));
        }
    }

    /**
     * Creates and starts a new TLS Server instance.
     *
     * @return The SecureServer instance
     * @throws Idscp2Exception If any error occurred during server creation/start
     */
    @Override
    public SecureServer listen(Idscp2Settings settings, SecureChannelInitListener channelInitListener,
                               CompletableFuture<ServerConnectionListener> serverListenerPromise) {
        try {
            return new TLSServer(settings, channelInitListener, serverListenerPromise);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new Idscp2Exception("Error while trying to to start SecureServer", e);
        }
    }
}
