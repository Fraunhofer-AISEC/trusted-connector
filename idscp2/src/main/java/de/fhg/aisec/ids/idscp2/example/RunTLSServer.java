package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;

import java.nio.file.Paths;
import java.util.Objects;

public class RunTLSServer {

    public static void main(String[] argv) {

        Idscp2Settings settings = new Idscp2Settings.Builder()
                .setKeyStorePath(Paths.get(Objects.requireNonNull(RunTLSServer.class.getClassLoader().
                        getResource("ssl/aisecconnector1-keystore.p12")).getPath()))
                .setTrustStorePath(Paths.get(Objects.requireNonNull(RunTLSServer.class.getClassLoader().
                        getResource("ssl/client-truststore_new.p12")).getPath()))
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setRatTimeoutDelay(300)
                .build();

        Idscp2ServerInitiator initiator = new Idscp2ServerInitiator();
        initiator.init(settings);
    }
}
