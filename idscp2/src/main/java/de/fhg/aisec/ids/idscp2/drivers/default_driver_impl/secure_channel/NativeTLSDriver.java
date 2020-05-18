package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client.TLSClient;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server.TLSServer;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of SecureChannelDriver interface on TLSv1.3
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class NativeTLSDriver implements SecureChannelDriver {
    private static final Logger LOG = LoggerFactory.getLogger(NativeTLSDriver.class);

    /*
     * Asynchronous client connect to a TLS server
     */
    @Override
    public void connect(Idscp2Settings settings, Idscp2Callback callback) {
        try {
            TLSClient tlsClient = new TLSClient(settings, callback);
            tlsClient.connect(settings.getHost(), settings.getServerPort());

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e){

            LOG.error("listen() failed. {}", e.getMessage());
            LOG.debug(Arrays.toString(e.getStackTrace()));
            callback.secureChannelConnectHandler(null);
        }
    }

    /*
     * Starting TLS Server
     *
     * return null on failure
     */
    @Override
    public SecureServer listen(Idscp2Settings settings, Idscp2Callback configCallback,
                               CompletableFuture<Idscp2Connection> connectionPromise) {
        try {
            TLSServer tlsServer = new TLSServer(settings, configCallback, connectionPromise);
            tlsServer.start();
            return tlsServer;

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e){
            LOG.error("listen() failed.", e);
        }

        return null;
    }
}
