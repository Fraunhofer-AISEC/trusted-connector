package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
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
public class Idscp2Configuration implements Idscp2Callback {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2Configuration.class);

    private final Idscp2EndpointListener user;
    private final DapsDriver dapsDriver;
    private final SecureChannelDriver secureChannelDriver;
    private final String[] localExpectedRatCipher;
    private final String[] localSupportedRatCipher;
    private final int ratTimeout;


    public Idscp2Configuration(Idscp2EndpointListener initiator,
                               DapsDriver dapsDriver,
                               SecureChannelDriver secureChannelDriver,
                               AttestationConfig expectedAttestation,
                               AttestationConfig supportedAttestation,
                               int ratTimeout
    ){
        this.user = initiator;
        this.dapsDriver = dapsDriver;
        this.secureChannelDriver = secureChannelDriver;
        this.localExpectedRatCipher = expectedAttestation.getRatMechanisms();
        this.localSupportedRatCipher = supportedAttestation.getRatMechanisms();
        this.ratTimeout = ratTimeout;
    }

    /*
     * User API to create a IDSCP2 connection as a client
     */
    public void connect(Idscp2Settings settings){
        LOG.info("Connect to an idscpv2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, this);
    }

    /*
     * User API to create a new IDSCP2 Server that starts a Secure Server that listens to new
     * secure channels
     */
    public Idscp2Server listen(Idscp2Settings settings) throws Idscp2Exception {
        LOG.info("Starting new IDSCP2 server at port {}", settings.getServerPort());

        // requires Promise<Idscp2Connection>
        final var connectionPromise = new CompletableFuture<Idscp2Connection>();
        final SecureServer secureServer;

        if ((secureServer = secureChannelDriver.listen(settings, this, connectionPromise)) == null) {
            throw new Idscp2Exception("Idscpv2 listen() failed. Cannot create SecureServer");
        }

        return new Idscp2Server(secureServer, connectionPromise);
    }

    /*
     * A callback implementation to receive a new established secure channel from an Secure client
     *
     * If the secure channel is null, no secure channel was established and an error is provided
     * to the user
     *
     * If the secure channel was established, a new FSM is created for this connection and the
     * IDSCP2 handshake is started. After a successful handshake, a new Idscp2Connection is
     * created and provided to the user
     */
    @Override
    public void secureChannelConnectHandler(SecureChannel secureChannel) {
        if (secureChannel == null){
            LOG.warn("IDSCP2 connect failed because no secure channel was established");
            user.onError("IDSCP2 connect failed because no secure channel was established");
        } else {
            LOG.debug("A new secure channel for an outgoing idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher,
                localExpectedRatCipher, ratTimeout);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (Idscp2Exception e){
                return;
            }

            String connectionId = UUID.randomUUID().toString();
            Idscp2Connection newConnection = new Idscp2Connection(fsm, connectionId);
            fsm.registerConnection(newConnection);
            LOG.info("A new IDSCP2 connection with id {} was created", connectionId);
            user.onConnection(newConnection);
        }
    }

    /*
     * A callback implementation to receive a new established secure channel from an Secure server
     *
     * If the secure channel is null, no secure channel was established and the result will be
     * ignored
     *
     * If the secure channel was established, a new FSM is created for this connection and the
     * IDSCP2 handshake is started. After a successful handshake, a new Idscp2Connection is
     * created and provided to the user and the IDSCP2 server
     */
    @Override
    public void secureChannelListenHandler(SecureChannel secureChannel,
                                           CompletableFuture<Idscp2Connection> connectionPromise) {
        if (secureChannel != null){
            LOG.debug("A new secure channel for an incoming idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher,
                localExpectedRatCipher, ratTimeout);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (Idscp2Exception e){
                return;
            }

            //create new Idscp2Connection
            String connectionId = UUID.randomUUID().toString();
            Idscp2Connection newConnection = new Idscp2Connection(fsm, connectionId);
            fsm.registerConnection(newConnection);
            LOG.info("A new idscpv2 connection with id {} was created", connectionId);
            // Complete the connection promise for the IDSCP server
            connectionPromise.complete(newConnection);
            user.onConnection(newConnection);
        } else {
            LOG.warn("An incoming idscpv2 client connection request failed because the secure channel is null.");
        }
    }
}
