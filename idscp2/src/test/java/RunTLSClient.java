import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;


public class RunTLSClient {

    public static void main(String[] args){
        //start client
        IDSCPv2Settings settings = new IDSCPv2Settings();
        settings.setKeyStorePath(RunTLSClient.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath());
        settings.setTrustStorePath(RunTLSClient.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath());

        IDSCPv2ClientInitiator initiator = new IDSCPv2ClientInitiator();
        initiator.init(settings);
    }
}
