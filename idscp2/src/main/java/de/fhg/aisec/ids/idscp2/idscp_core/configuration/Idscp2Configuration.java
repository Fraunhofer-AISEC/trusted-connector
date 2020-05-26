package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.ServerConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Idscp2Configuration class, provides IDSCP2 API to the User (Idscp2EndpointListener)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2Configuration implements SecureChannelInitListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2Configuration.class);

    private final Idscp2EndpointListener endpointListener;
    private final Idscp2Settings settings;
    private final DapsDriver dapsDriver;
    private final SecureChannelDriver secureChannelDriver;

    public Idscp2Configuration(Idscp2EndpointListener endpointListener,
                               Idscp2Settings settings,
                               DapsDriver dapsDriver,
                               SecureChannelDriver secureChannelDriver
    ) {
        this.endpointListener = endpointListener;
        this.settings = settings;
        this.dapsDriver = dapsDriver;
        this.secureChannelDriver = secureChannelDriver;
    }

    /**
     * User API to create a IDSCP2 connection as a client
     */
    public void connect(Idscp2Settings settings) {
        LOG.info("Connect to an IDSCP2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, this);
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
        if (secureChannel == null) {
            LOG.warn("IDSCP2 connect failed because no secure channel available");
            endpointListener.onError("IDSCP2 connect failed because no secure channel available");
        } else {
            LOG.trace("A new secure channel for an IDSCP2 connection was established");
            FSM fsm = new FSM(
                    secureChannel,
                    dapsDriver,
                    settings.getSupportedAttestation().getRatMechanisms(),
                    settings.getExpectedAttestation().getRatMechanisms(),
                    settings.getRatTimeoutDelay());

            try {
                // Blocking until handshake is done
                fsm.startIdscpHandshake();
            } catch (Idscp2Exception e) {
                return;
            }

            String connectionId = UUID.randomUUID().toString();
            // Threads calling onMessage() will be blocked until all listeners have been registered, see below
            Idscp2Connection newConnection = new Idscp2Connection(fsm, connectionId);
            fsm.registerConnection(newConnection);
            LOG.debug("A new IDSCP2 connection with id {} was created", connectionId);
            if (serverListenerPromise != null) {
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
            }
            endpointListener.onConnection(newConnection);
            newConnection.unlockMessaging();
        }
    }

}
