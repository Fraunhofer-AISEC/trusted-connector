package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client.TLSClient;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server.TLSServer;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;
import de.fhg.aisec.ids.idscp2.idscp_core.idscp_server.IdscpConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * An implementation of SecureChannelDriver interface on TLSv1.3
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class NativeTLSDriver implements SecureChannelDriver {
    private static final Logger LOG = LoggerFactory.getLogger(NativeTLSDriver.class);

    @Override
    public void connect(IDSCPv2Settings settings, IDSCPv2Callback callback) {
        try {
            TLSClient tlsClient = new TLSClient(settings, callback);
            tlsClient.connect(settings.getHost(), settings.getServerPort());

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e){

            LOG.error("listen() failed. {}", e.getMessage());
            LOG.debug(Arrays.toString(e.getStackTrace()));
            callback.secureChannelConnectHandler(null);
        }
    }

    @Override
    public SecureServer listen(IDSCPv2Settings settings, IDSCPv2Callback configCallback,
                               IdscpConnectionListener idscpServerCallback) {
        try {
            TLSServer tlsServer = new TLSServer(settings, configCallback, idscpServerCallback);
            tlsServer.start();
            return tlsServer;

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e){
            LOG.error("listen() failed. {}", e.getMessage());
            LOG.debug(Arrays.toString(e.getStackTrace()));
        }

        return null;
    }
}
