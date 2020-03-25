import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

public class RunTLSServer{

    public static void main(String[] argv){

        IDSCPv2Settings settings = new IDSCPv2Settings.Builder()
            .setKeyStore(RunTLSClient.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStore(RunTLSServer.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setCertificateAlias("1.0.1")
            .setDapsKeyAlias("1")
            .build();

        IDSCPv2ServerInitiator initiator = new IDSCPv2ServerInitiator();
        initiator.init(settings);
    }
}
