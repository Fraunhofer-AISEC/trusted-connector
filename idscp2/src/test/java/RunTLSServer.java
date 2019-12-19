import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

public class RunTLSServer{

    public static void main(String[] argv){

        IDSCPv2Settings settings = new IDSCPv2Settings();
        settings.setKeyStorePath(RunTLSServer.class.getClassLoader().
                getResource("ssl/aisecconnector2-keystore.jks").getPath());
        settings.setTrustStorePath(RunTLSServer.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath());

        IDSCPv2ServerInitiator initiator = new IDSCPv2ServerInitiator();
        initiator.init(settings);
    }
}
