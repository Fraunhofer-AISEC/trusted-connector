package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IDSCPv2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpServerListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * IDSCPv2Configuration class, provides IDSCPv2 API to the User (IDSCPv2Initiator)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Configuration implements IDSCPv2Callback {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Configuration.class);

    private final IDSCPv2Initiator user;
    private final DapsDriver dapsDriver;
    private final SecureChannelDriver secureChannelDriver;
    private final String[] localExpectedRatCipher;
    private final String[] localSupportedRatCipher;
    private final int ratTimeout;


    public IDSCPv2Configuration(IDSCPv2Initiator initiator,
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
     * User API to create a IDSCPv2 connection as a client
     */
    public void connect(IDSCPv2Settings settings){
        LOG.info("Connect to an idscpv2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, this);
    }

    /*
     * User API to create a new IDSCPv2 Server that starts a Secure Server that listens to new
     * secure channels
     */
    public IDSCPv2Server listen(IDSCPv2Settings settings) throws IDSCPv2Exception {
        LOG.info("Starting new IDSCPv2 server at port {}", settings.getServerPort());

        final SecureServer secureServer;
        final IDSCPv2Server idscpServer = new IDSCPv2Server();

        if ((secureServer = secureChannelDriver.listen(settings, this, idscpServer)) == null){
            throw new IDSCPv2Exception("Idscpv2 listen() failed. Cannot create SecureServer");
        }

        idscpServer.setSecureServer(secureServer);
        return idscpServer;
    }

    /*
     * A callback implementation to receive a new established secure channel from an Secure client
     *
     * If the secure channel is null, no secure channel was established and an error is provided
     * to the user
     *
     * If the secure channel was established, a new FSM is created for this connection and the
     * IDSCPv2 handshake is started. After a successful handshake, a new Idscp2Connection is
     * created and provided to the user
     */
    @Override
    public void secureChannelConnectHandler(SecureChannel secureChannel) {
        if (secureChannel == null){
            LOG.warn("IDSCPv2 connect failed because no secure channel was established");
            user.errorHandler("IDSCPv2 connect failed because no secure channel was established");
        } else {
            LOG.debug("A new secure channel for an outgoing idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher,
                localExpectedRatCipher, ratTimeout);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (IDSCPv2Exception e){
                return;
            }

            String connectionId = UUID.randomUUID().toString();
            Idscp2Connection newConnection = new Idscp2Connection(fsm, connectionId);
            fsm.registerConnection(newConnection);
            LOG.info("A new IDSCPv2 connection with id {} was created", connectionId);
            user.newConnectionHandler(newConnection);
        }
    }

    /*
     * A callback implementation to receive a new established secure channel from an Secure server
     *
     * If the secure channel is null, no secure channel was established and the result will be
     * ignored
     *
     * If the secure channel was established, a new FSM is created for this connection and the
     * IDSCPv2 handshake is started. After a successful handshake, a new Idscp2Connection is
     * created and provided to the user and the IDSCPv2 server
     */
    @Override
    public void secureChannelListenHandler(SecureChannel secureChannel, IdscpServerListener idscpServer) {
        if (secureChannel != null){
            LOG.debug("A new secure channel for an incoming idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher,
                localExpectedRatCipher, ratTimeout);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (IDSCPv2Exception e){
                return;
            }

            //create new Idscp2Connection
            String connectionId = UUID.randomUUID().toString();
            Idscp2Connection newConnection = new Idscp2Connection(fsm, connectionId);
            fsm.registerConnection(newConnection);
            LOG.info("A new idscpv2 connection with id {} was created", connectionId);
            idscpServer.onConnect(newConnection); //bind connection to idscp server
            user.newConnectionHandler(newConnection);
        } else {
            LOG.warn("An incoming idscpv2 client connection request failed because the secure channel is null.");
        }
    }
}
