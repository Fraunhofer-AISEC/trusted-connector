import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

import java.util.concurrent.ConcurrentHashMap;

public class IDSCPv2ClientInitiator implements IDSCPv2Initiator {

    private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();

    public void init(IDSCPv2Settings settings){
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();
        IDSCPv2Configuration clientConfig = new IDSCPv2Configuration(this,
                null, null, null, secureChannelDriver);
        clientConfig.connect(settings);
    }


    @Override
    public void newConnectionHandler(IDSCPv2Connection connection) {
        this.connections.put(connection.getConnectionId(), connection);
        //connection.close();
    }

    @Override
    public void errorHandler(String error) {
        System.out.println("Error occurred: " + error);
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        System.out.println("Connection closed");
        connections.remove(connectionId);
    }
}
