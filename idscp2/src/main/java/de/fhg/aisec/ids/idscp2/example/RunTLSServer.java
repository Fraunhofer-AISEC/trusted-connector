package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;

import java.util.Objects;

public class RunTLSServer {

    public static void main(String[] argv) {

        Idscp2Settings settings = new Idscp2Settings.Builder()
                .setKeyStorePath(Objects.requireNonNull(RunTLSServer.class.getClassLoader().
                        getResource("ssl/aisecconnector1-keystore.p12")).getPath())
                .setTrustStorePath(Objects.requireNonNull(RunTLSServer.class.getClassLoader().
                        getResource("ssl/client-truststore_new.p12")).getPath())
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setRatTimeoutDelay(300)
                .build();

        Idscp2ServerInitiator initiator = new Idscp2ServerInitiator();
        initiator.init(settings);
    }
}
