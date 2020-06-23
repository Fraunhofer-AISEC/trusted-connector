package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Idscp2ServerFactory class, provides IDSCP2 API to the User (Idscp2EndpointListener)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2ServerFactory implements SecureChannelInitListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ServerFactory.class);

    private final Idscp2EndpointListener endpointListener;
    private final Idscp2Settings settings;
    private final DapsDriver dapsDriver;
    private final SecureChannelDriver secureChannelDriver;

    public Idscp2ServerFactory(Idscp2EndpointListener endpointListener,
                               Idscp2Settings settings,
                               DapsDriver dapsDriver,
                               SecureChannelDriver secureChannelDriver
    ) {
        this.endpointListener = endpointListener;
        this.settings = settings;
        this.dapsDriver = dapsDriver;
        this.secureChannelDriver = secureChannelDriver;
    }

    /*
     * User API to create a new IDSCP2 Server that starts a Secure Server that listens to new
     * secure channels
     */
    public Idscp2Server listen(Idscp2Settings settings) throws Idscp2Exception {
        LOG.info("Starting new IDSCP2 server at port {}", settings.getServerPort());

        final var serverListenerPromise = new CompletableFuture<ServerConnectionListener>();
        final var secureServer = secureChannelDriver.listen(settings, this, serverListenerPromise);
        final var server = new Idscp2Server(secureServer);
        serverListenerPromise.complete(server);
        return server;
    }

    /**
     * A callback implementation to receive a new established secure channel from an Secure client/server.
     * <p>
     * If the secure channel is null, no secure channel was established and an error is provided
     * to the user (or the error is ignored, in server case).
     * <p>
     * If the secure channel was established, a new FSM is created for this connection and the
     * IDSCP2 handshake is started. After a successful handshake, a new Idscp2Connection is
     * created and provided to the user (and the IDSCP2 server).
     */
    @Override
    public synchronized void onSecureChannel(SecureChannel secureChannel,
                                             CompletableFuture<ServerConnectionListener> serverListenerPromise) {
        LOG.trace("A new secure channel for an IDSCP2 connection was established");
        // Threads calling onMessage() will be blocked until all listeners have been registered, see below
        Idscp2Connection newConnection = new Idscp2Connection(secureChannel, settings, dapsDriver);
        // Complete the connection promise for the IDSCP server
        serverListenerPromise.thenAccept(serverListener -> {
            serverListener.onConnectionCreated(newConnection);
            newConnection.addConnectionListener(new Idscp2ConnectionAdapter() {
                @Override
                public void onClose(Idscp2Connection connection) {
                    serverListener.onConnectionClose(connection);
                }
            });
        });
        endpointListener.onConnection(newConnection);
        // Listeners have been applied in onConnection() callback above, so we can safely unlock messaging now
        newConnection.unlockMessaging();
    }

    @Override
    public void onError(Throwable t) {
        endpointListener.onError(t);
    }

}
