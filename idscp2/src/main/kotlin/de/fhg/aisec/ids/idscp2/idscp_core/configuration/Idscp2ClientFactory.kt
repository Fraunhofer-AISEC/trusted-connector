package de.fhg.aisec.ids.idscp2.idscp_core.configuration

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Idscp2ServerFactory class, provides IDSCP2 API to the User (Idscp2EndpointListener)
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2ClientFactory(private val dapsDriver: DapsDriver,
                          private val secureChannelDriver: SecureChannelDriver
) {
    /**
     * User API to create a IDSCP2 connection as a client
     */
    fun connect(settings: Idscp2Settings, connectionFuture: CompletableFuture<Idscp2Connection>) {
        LOG.info("Connect to an IDSCP2 server ({})", settings.host)
        secureChannelDriver.connect(settings, dapsDriver, connectionFuture)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientFactory::class.java)
    }
}