package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;

import java.nio.charset.StandardCharsets;

public class Idscp2ClientInitiator implements Idscp2EndpointListener {

    public void init(Idscp2Settings settings){
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();
        DefaultDapsDriverConfig config =
            new DefaultDapsDriverConfig.Builder()
                .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
                .setKeyStorePath(settings.getKeyStorePath())
                .setTrustStorePath(settings.getTrustStorePath())
                .setKeyStorePassword(settings.getKeyStorePassword())
                .setTrustStorePassword(settings.getTrustStorePassword())
                .setKeyAlias(settings.getDapsKeyAlias())
                .setDapsUrl("https://daps.aisec.fraunhofer.de")
                .build();

        DapsDriver dapsDriver = new DefaultDapsDriver(config);

        RatProverDriverRegistry.getInstance().registerDriver(
            "Dummy", RatProverDummy.class, null);
        RatVerifierDriverRegistry.getInstance().registerDriver(
            "Dummy", RatVerifierDummy.class, null);

        RatProverDriverRegistry.getInstance().registerDriver(
            "TPM2d", TPM2dProver.class,
            new TPM2dProverConfig.Builder().build()
        );
        RatVerifierDriverRegistry.getInstance().registerDriver(
            "TPM2d", TPM2dVerifier.class,
            new TPM2dVerifierConfig.Builder().build()
        );

        Idscp2Configuration clientConfig = new Idscp2Configuration(
            this,
            dapsDriver,
            secureChannelDriver,
            settings.getExpectedAttestation(),
            settings.getSupportedAttestation(),
            settings.getRatTimeoutDelay()
        );
        clientConfig.connect(settings);
    }

    @Override
    public void onConnection(Idscp2Connection connection) {
        System.out.println("Client: New connection with id " + connection.getConnectionId());
        connection.addConnectionListener(new Idscp2ConnectionListener() {
            @Override
            public void onError(String error) {
                System.out.println("Client connection error occurred: " + error);
            }

            @Override
            public void onClose(String connectionId) {
                System.out.println("Client: Connection with id " + connectionId + " has been closed");
            }
        });
        connection.addGenericMessageListener(((c, type, data) -> System.out.println(
            "Received message of type \"" + type + "\":\n" + new String(data, StandardCharsets.UTF_8))));
        // Register listener for server reply
        connection.addMessageListener("pong", (c, type, data) ->
            System.out.println("Received pong message: " + new String(data, StandardCharsets.UTF_8)));
        System.out.println("Sending PING...");
        connection.send("ping", "PING".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void onError(String error) {
        System.out.println("Client endpoint error occurred: " + error);
    }
}
