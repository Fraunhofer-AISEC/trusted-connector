package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IDSCPv2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * IDSCPv2Configuration class, provides IDSCPv2 API to the User (IDSCPv2Initiator)
 *
 * User API
 *
 * Methods:
 * void connect(IDSCPv2Settings)                    initiate idscpv2 connect to host from settings
 * SecureServer listen(IDSCPv2Settings)             initiate creation of a
 * void secureChannelConnectHandler(SecureChannel)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Configuration implements IDSCPv2Callback {
    private static final Logger LOG = LoggerFactory.getLogger(IDSCPv2Configuration.class);

    private IDSCPv2Initiator user;
    private DapsDriver dapsDriver;
    private SecureChannelDriver secureChannelDriver;
    private final String[] localExpectedRatCipher;
    private final String[] localSupportedRatCipher;


    public IDSCPv2Configuration(IDSCPv2Initiator initiator,
                                DapsDriver dapsDriver,
                                SecureChannelDriver secureChannelDriver,
                                AttestationConfig expectedAttestation,
                                AttestationConfig supportedAttestation){
        this.user = initiator;
        this.dapsDriver = dapsDriver;
        this.secureChannelDriver = secureChannelDriver;
        this.localExpectedRatCipher = expectedAttestation.getRatMechanisms();
        this.localSupportedRatCipher = supportedAttestation.getRatMechanisms();
    }

    public void connect(IDSCPv2Settings settings){
        LOG.info("Connect to an idscpv2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, this);
    }

    public IDSCPv2Server listen(IDSCPv2Settings settings) throws IDSCPv2Exception {
        LOG.info("Starting new IDSCPv2 server at port {}", settings.getServerPort());

        SecureServer secureServer;
        IDSCPv2Server idscpServer = new IDSCPv2Server();

        if ((secureServer = secureChannelDriver.listen(settings, this, idscpServer)) == null){
            throw new IDSCPv2Exception("Idscpv2 listen() failed. Cannot create SecureServer");
        }

        idscpServer.setSecureServer(secureServer);
        return idscpServer;
    }

    @Override
    public void secureChannelConnectHandler(SecureChannel secureChannel) {
        if (secureChannel == null){
            LOG.warn("IDSCPv2 connect failed because no secure channel was established");
            user.errorHandler("IDSCPv2 connect failed because no secure channel was established");
        } else {
            LOG.debug("A new secure channel for an outgoing idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher, localExpectedRatCipher);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (IDSCPv2Exception e){
                return;
            }

            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(fsm, connectionId, user, null);
            fsm.registerMessageListener(newConnection);
            LOG.info("A new IDSCPv2 connection with id {} was created", connectionId);
            user.newConnectionHandler(newConnection);
        }
    }

    @Override
    public void secureChannelListenHandler(SecureChannel secureChannel, IdscpConnectionListener idscpServer) {
        if (secureChannel != null){
            LOG.debug("A new secure channel for an incoming idscpv2 connection was established");
            FSM fsm = new FSM(secureChannel, dapsDriver, localSupportedRatCipher, localExpectedRatCipher);

            try {
                fsm.startIdscpHandshake(); //blocking until handshake is done
            } catch (IDSCPv2Exception e){
                return;
            }

            //create new IDSCPv2Connection
            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(fsm, connectionId, user, idscpServer);
            fsm.registerMessageListener(newConnection);
            LOG.info("A new idscpv2 connection with id {} was created", connectionId);
            idscpServer.newConnectionHandler(newConnection); //bind connection to idscp server
            user.newConnectionHandler(newConnection);
        } else {
            LOG.warn("An incoming idscpv2 client connection request failed because no secure channel was established");
        }
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        LOG.info("IDSCPv2 connection with id {} has been closed", connectionId);
        user.connectionClosedHandler(connectionId);
    }
}
