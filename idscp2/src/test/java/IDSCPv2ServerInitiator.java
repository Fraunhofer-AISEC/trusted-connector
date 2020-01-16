import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2ProverImpl;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2VerifierImpl;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IDSCPv2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

import java.util.concurrent.ConcurrentHashMap;


public class IDSCPv2ServerInitiator implements IDSCPv2Initiator {

    //private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();

    public void init(IDSCPv2Settings serverSettings)  {
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();
        DapsDriver dapsDriver = new DefaultDapsDriver();
        RatVerifierDriver ratVerifier = new TPM2VerifierImpl();
        RatProverDriver ratProver = new TPM2ProverImpl();

        IDSCPv2Configuration idscpServerConfig = new IDSCPv2Configuration(this,
                dapsDriver, ratVerifier, ratProver, secureChannelDriver);
        IDSCPv2Server idscPv2Server;
        try {
            idscPv2Server = idscpServerConfig.listen(serverSettings);
        } catch (IDSCPv2Exception e) {
            //e.printStackTrace();
            return;
        }
        //secureServer.safeStop();
        try {
            Thread.sleep(120000); //run server for 2 minutes
        } catch (Exception e){
            return;
        }
        idscPv2Server.terminate();
    }

    @Override
    public void newConnectionHandler(IDSCPv2Connection connection) {
        //connections.put(connection.getConnectionId(), connection);
    }

    @Override
    public void errorHandler(String error) {
        System.out.println("Error occurred: " + error);
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        System.out.println("Connection with id " + connectionId + " has been closed");
        //connections.remove(connectionId);
    }
}
