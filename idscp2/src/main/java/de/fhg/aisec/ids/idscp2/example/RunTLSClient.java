package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;

import java.util.Objects;

public class RunTLSClient {

    public static void main(String[] args) {

        Idscp2Settings settings = new Idscp2Settings.Builder()
                .setKeyStorePath(Objects.requireNonNull(RunTLSClient.class.getClassLoader().
                        getResource("ssl/aisecconnector1-keystore.p12")).getPath())
                .setTrustStorePath(Objects.requireNonNull(RunTLSClient.class.getClassLoader().
                        getResource("ssl/client-truststore_new.p12")).getPath())
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setRatTimeoutDelay(300)
                .build();

        Idscp2ClientInitiator initiator = new Idscp2ClientInitiator();
        initiator.init(settings);
    }
}
