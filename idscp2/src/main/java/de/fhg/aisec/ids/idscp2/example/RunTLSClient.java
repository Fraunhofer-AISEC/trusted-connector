package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;


public class RunTLSClient {

    public static void main(String[] args){
        //start client
        Idscp2Settings settings = new Idscp2Settings.Builder()
            .setKeyStore(RunTLSClient.class.getClassLoader().
                getResource("ssl/aisecconnector1-keystore.jks").getPath())
            .setTrustStore(RunTLSClient.class.getClassLoader().
                getResource("ssl/client-truststore_new.jks").getPath())
            .setCertificateAlias("1.0.1")
            .setDapsKeyAlias("1")
            .setRatTimeoutDelay(10)
            .build();

        Idscp2ClientInitiator initiator = new Idscp2ClientInitiator();
        initiator.init(settings);
    }
}
