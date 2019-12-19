package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

import java.util.UUID;

/**
 * IDSCPv2Configuration Class, provides IDSCPv2 API to the User (IDSCPv2Initiator)
 *
 * Developer API
 *
 * Methods:
 * void onConnect(IDSCPv2Connection)       to notify the user, a new connection was created
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Configuration implements IDSCPv2Callback {

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
        secureChannelDriver.connect(settings, this);
    }

    public SecureServer listen(IDSCPv2Settings settings){
        return secureChannelDriver.listen(settings, this);
    }

    @Override
    public void secureChannelConnectHandler(SecureChannel secureChannel) {
        if (secureChannel == null){
            user.errorHandler("Creating Secure channel failed. Cannot create secure Channel");
        } else {

            //toDo verify security properties
            //toDo RAT

            //create new IDSCPv2Connection
            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(secureChannel, connectionId);
            secureChannel.registerMessageListener(newConnection);
            user.newConnectionHandler(newConnection);
        }
    }

    @Override
    public void secureChannelListenHandler(SecureChannel secureChannel) {
        if (secureChannel != null){

            //toDo verify security properties
            //toDo RAT

            //create new IDSCPv2Connection
            String connectionId = UUID.randomUUID().toString();
            IDSCPv2Connection newConnection = new IDSCPv2Connection(secureChannel, connectionId);
            secureChannel.registerMessageListener(newConnection);
            user.newConnectionHandler(newConnection);
        }
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        user.connectionClosedHandler(connectionId);
    }
}
