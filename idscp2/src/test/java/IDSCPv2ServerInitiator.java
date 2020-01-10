import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

import java.util.concurrent.ConcurrentHashMap;


public class IDSCPv2ServerInitiator implements IDSCPv2Initiator {

    private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();
    private SecureServer secureServer = null;


    public void init(IDSCPv2Settings serverSettings)  {
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();
        IDSCPv2Configuration idscpServerConfig = new IDSCPv2Configuration(this,
                null,null,null,secureChannelDriver);
        try {
            secureServer = idscpServerConfig.listen(serverSettings);
        } catch (IDSCPv2Exception e) {
            //e.printStackTrace();
            return;
        }
        //secureServer.safeStop();
        try {
            Thread.sleep(300000); //run server for 5 minutes
        } catch (Exception e){

        }
        secureServer.safeStop();
    }

    @Override
    public void newConnectionHandler(IDSCPv2Connection connection) {
        connections.put(connection.getConnectionId(), connection);
    }

    @Override
    public void errorHandler(String error) {

    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        System.out.println("Connection closed");
        connections.remove(connectionId);
    }
}
