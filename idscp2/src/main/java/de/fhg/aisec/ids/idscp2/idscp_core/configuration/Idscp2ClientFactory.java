package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Idscp2ServerFactory class, provides IDSCP2 API to the User (Idscp2EndpointListener)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2ClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(Idscp2ClientFactory.class);

    private final DapsDriver dapsDriver;
    private final SecureChannelDriver secureChannelDriver;

    public Idscp2ClientFactory(DapsDriver dapsDriver,
                               SecureChannelDriver secureChannelDriver
    ) {
        this.dapsDriver = dapsDriver;
        this.secureChannelDriver = secureChannelDriver;
    }

    /**
     * User API to create a IDSCP2 connection as a client
     */
    public void connect(Idscp2Settings settings, CompletableFuture<Idscp2Connection> connectionFuture) {
        LOG.info("Connect to an IDSCP2 server ({})", settings.getHost());
        secureChannelDriver.connect(settings, dapsDriver, connectionFuture);
    }

}
