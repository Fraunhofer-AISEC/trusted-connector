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
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionListener;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;

import java.nio.charset.StandardCharsets;


public class Idscp2ServerInitiator implements Idscp2EndpointListener {

    public void init(Idscp2Settings serverSettings)  {
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();

        DefaultDapsDriverConfig config =
            new DefaultDapsDriverConfig.Builder()
                .setConnectorUUID("edc5d7b3-a398-48f0-abb0-3751530c4fed")
                .setKeyStorePath(serverSettings.getKeyStorePath())
                .setTrustStorePath(serverSettings.getTrustStorePath())
                .setKeyStorePassword(serverSettings.getKeyStorePassword())
                .setTrustStorePassword(serverSettings.getTrustStorePassword())
                .setKeyAlias(serverSettings.getDapsKeyAlias())
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

        Idscp2Configuration idscpServerConfig = new Idscp2Configuration(
            this,
            dapsDriver,
            secureChannelDriver,
            serverSettings.getExpectedAttestation(),
            serverSettings.getSupportedAttestation(),
            serverSettings.getRatTimeoutDelay()
        );

        Idscp2Server idscp2Server;
        try {
            idscp2Server = idscpServerConfig.listen(serverSettings);
        } catch (Idscp2Exception e) {
            //e.printStackTrace();
            return;
        }
        //secureServer.safeStop();
        try {
            Thread.sleep(120000); //run server for 2 minutes
        } catch (Exception e){
            return;
        }
        idscp2Server.terminate();
    }

    @Override
    public void onConnection(Idscp2Connection connection) {
        System.out.println("Server: New connection with id " + connection.getConnectionId());
        connection.addConnectionListener(new Idscp2ConnectionListener() {
            @Override
            public void onError(String error) {
                System.out.println("Server connection error occurred: " + error);
            }

            @Override
            public void onClose(String connectionId) {
                System.out.println("Server: Connection with id " + connectionId + " has been closed");
            }
        });
        connection.addGenericMessageListener(((type, data) -> System.out.println(
            "Received message of type \"" + type + "\":\n" + new String(data, StandardCharsets.UTF_8))));
        connection.addMessageListener("ping", ((type, data) -> {
            System.out.println("Received ping message:\n" + new String(data, StandardCharsets.UTF_8));
            System.out.println("Sending PONG...");
            connection.send("pong", "PONG".getBytes(StandardCharsets.UTF_8));
        }));
    }

    @Override
    public void onError(String error) {
        System.out.println("Server endpoint error occurred: " + error);
    }
}
