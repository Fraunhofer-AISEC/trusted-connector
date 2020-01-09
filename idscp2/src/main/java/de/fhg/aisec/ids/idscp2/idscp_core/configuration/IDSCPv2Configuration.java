package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;
    private SecureChannelDriver secureChannelDriver;


    public IDSCPv2Configuration(IDSCPv2Initiator initiator,
                                DapsDriver dapsDriver,
                                RatVerifierDriver ratVerifierDriver,
                                RatProverDriver ratProverDriver,
                                SecureChannelDriver secureChannelDriver){
        this.user = initiator;
        this.dapsDriver = dapsDriver;
        this.ratVerifierDriver = ratVerifierDriver;
        this.ratProverDriver = ratProverDriver;
        this.secureChannelDriver = secureChannelDriver;
    }

    public void connect(IDSCPv2Settings settings){
        LOG.info("Connect to an idscpv2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, this);
    }

    public SecureServer listen(IDSCPv2Settings settings) throws IDSCPv2Exception {
        LOG.info("Starting new IDSCPv2 server at port {}", settings.getServerPort());

        SecureServer secureServer;
        if ((secureServer = secureChannelDriver.listen(settings, this)) == null){
            throw new IDSCPv2Exception("Idscpv2 listen() failed. Cannot create SecureServer");
        }

        return secureServer;
    }

    @Override
    public void secureChannelConnectHandler(SecureChannel secureChannel) {
        if (secureChannel == null){
            LOG.warn("IDSCPv2 connect failed because no secure channel was established");
            user.errorHandler("IDSCPv2 connect failed because no secure channel was established");
        } else {
            LOG.debug("A new secure channel for an outgoing idscpv2 connection was established");
            LOG.debug("Send Idscp hello");
            secureChannel.send(IDSCPv2.IdscpMessage.newBuilder()
                    .setType(IDSCPv2.IdscpMessage.Type.IDSCP_HELLO)
                    .setIdscpHello(IDSCPv2.IdscpHello.newBuilder().build()).build());
            //toDo verify security properties
            //toDo RAT

            LOG.debug("All IDSCPv2 requirements for new connection were fulfilled");
            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(secureChannel, connectionId);
            secureChannel.registerMessageListener(newConnection);
            LOG.info("A new IDSCPv2 connection with id {} was created", connectionId);
            user.newConnectionHandler(newConnection);
        }
    }

    @Override
    public void secureChannelListenHandler(SecureChannel secureChannel) {
        if (secureChannel != null){
            LOG.debug("A new secure channel for an incoming idscpv2 connection was established");
            LOG.debug("Send Idscp hello");
            secureChannel.send(IDSCPv2.IdscpMessage.newBuilder()
                    .setType(IDSCPv2.IdscpMessage.Type.IDSCP_HELLO)
                    .setIdscpHello(IDSCPv2.IdscpHello.newBuilder().build()).build());
            //toDo verify security properties
            //toDo RAT

            LOG.debug("All IDSCPv2 requirements for a new incoming connection were fulfilled");
            //create new IDSCPv2Connection
            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(secureChannel, connectionId);
            secureChannel.registerMessageListener(newConnection);
            LOG.info("A new idscpv2 connection with id {} was created", connectionId);
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
