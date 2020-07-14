package de.fhg.aisec.ids.idscp2.example;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ClientFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Idscp2ClientInitiator {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ClientInitiator.class);

    private final CompletableFuture<Idscp2Connection> connectionFuture = new CompletableFuture<>();

    public void init(Idscp2Settings settings) {
        SecureChannelDriver secureChannelDriver = new NativeTLSDriver();

        DefaultDapsDriverConfig config =
                new DefaultDapsDriverConfig.Builder()
                        .setKeyStorePath(settings.getKeyStorePath())
                        .setTrustStorePath(settings.getTrustStorePath())
                        .setKeyStorePassword(settings.getKeyStorePassword())
                        .setTrustStorePassword(settings.getTrustStorePassword())
                        .setKeyAlias(settings.getDapsKeyAlias())
                        .setDapsUrl("https://daps.aisec.fraunhofer.de")
                        .build();

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

        final var clientConfig = new Idscp2ClientFactory(
                new DefaultDapsDriver(config),
                secureChannelDriver
        );

        clientConfig.connect(settings, connectionFuture);
        connectionFuture.thenAccept(connection -> {
            System.out.println("Client: New connection with id " + connection.getId());
            connection.addConnectionListener(new Idscp2ConnectionAdapter() {
                @Override
                public void onError(Throwable t) {
                    LOG.error("Client connection error occurred", t);
                }

                @Override
                public void onClose(Idscp2Connection connection) {
                    LOG.info("Client: Connection with id " + connection.getId() + " has been closed");
                }
            });
            connection.addGenericMessageListener(((c, type, data) -> System.out.println(
                    "Received message of type \"" + type + "\":\n" + new String(data, StandardCharsets.UTF_8))));
            connection.addMessageListener("ping", (c, type, data) -> {
                System.out.println("Received ping message: " + new String(data, StandardCharsets.UTF_8));
                CompletableFuture.runAsync(c::close);  // FSM error if run from the same thread
            });
            connection.unlockMessaging();
            System.out.println("Sending PING...");
            connection.send("ping", "PING".getBytes(StandardCharsets.UTF_8));
        }).exceptionally(t -> {
            LOG.error("Client endpoint error occurred", t);
            return null;
        });
    }
}
