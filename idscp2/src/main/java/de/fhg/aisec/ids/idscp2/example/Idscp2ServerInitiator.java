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
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionAdapter;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ServerFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.Idscp2Server;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class Idscp2ServerInitiator implements Idscp2EndpointListener {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ServerInitiator.class);

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

        Idscp2ServerFactory serverConfig = new Idscp2ServerFactory(
                this,
                settings,
                dapsDriver,
                secureChannelDriver
        );

        Idscp2Server idscp2Server = serverConfig.listen(settings);

//        try {
//            Thread.sleep(40_000); //run server for 2 minutes
//        } catch (Exception ignored) {
//        } finally {
//            idscp2Server.closeConnection();
//        }
    }

    @Override
    public void onConnection(Idscp2Connection connection) {
        System.out.println("Server: New connection with id " + connection.getId());
        connection.addConnectionListener(new Idscp2ConnectionAdapter() {
            @Override
            public void onError(Throwable t) {
                LOG.error("Server connection error occurred", t);
            }

            @Override
            public void onClose(Idscp2Connection connection) {
                LOG.info("Server: Connection with id " + connection.getId() + " has been closed");
            }
        });
        connection.addGenericMessageListener(((c, type, data) -> System.out.println(
                "Received message of type \"" + type + "\":\n" + new String(data, StandardCharsets.UTF_8))));
        connection.addMessageListener("ping", ((c, type, data) -> {
            System.out.println("Received ping message:\n" + new String(data, StandardCharsets.UTF_8));
            System.out.println("Sending PONG...");
            c.send("ping", "PONG".getBytes(StandardCharsets.UTF_8));
        }));
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("Server endpoint error occurred", t);
    }
}
