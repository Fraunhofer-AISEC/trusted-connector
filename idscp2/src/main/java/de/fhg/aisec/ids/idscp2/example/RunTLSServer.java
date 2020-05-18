package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;

public class RunTLSServer {

    public static void main(String[] argv){

        Idscp2Settings settings = new Idscp2Settings.Builder()
            .setKeyStore(RunTLSClient.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStore(RunTLSServer.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setCertificateAlias("1.0.1")
            .setDapsKeyAlias("1")
            .setRatTimeoutDelay(14)
            .build();

        Idscp2ServerInitiator initiator = new Idscp2ServerInitiator();
        initiator.init(settings);
    }
}
