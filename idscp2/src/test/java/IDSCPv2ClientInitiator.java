import de.fhg.aisec.ids.idscp2.IDSCPv2Initiator;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d.TPM2dProver;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d.TPM2dVerifier;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d.Tpm2dProverConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d.Tpm2dVerifierConfig;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Configuration;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;

import java.util.concurrent.ConcurrentHashMap;

public class IDSCPv2ClientInitiator implements IDSCPv2Initiator {

    private ConcurrentHashMap<String, IDSCPv2Connection> connections = new ConcurrentHashMap<>();

    public void init(IDSCPv2Settings settings){
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
            new Tpm2dProverConfig.Builder().build()
        );
        RatVerifierDriverRegistry.getInstance().registerDriver(
            "TPM2d", TPM2dVerifier.class,
            new Tpm2dVerifierConfig.Builder().build()
        );

        IDSCPv2Configuration clientConfig = new IDSCPv2Configuration(
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
    public void newConnectionHandler(IDSCPv2Connection connection) {
        this.connections.put(connection.getConnectionId(), connection);
        System.out.println("User: New connection with id " + connection.getConnectionId());
    }

    @Override
    public void errorHandler(String error) {
        System.out.println("User: Error occurred: " + error);
    }

    @Override
    public void connectionClosedHandler(String connectionId) {
        System.out.println("User: Connection closed");
        connections.remove(connectionId);
    }
}
