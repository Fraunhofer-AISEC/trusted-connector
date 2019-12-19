package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.client.TLSClient;
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.server.TLSServer;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureServer;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;


public class NativeTLSDriver implements SecureChannelDriver {

    @Override
    public void connect(IDSCPv2Settings settings, IDSCPv2Callback callback) {
        TLSClient tlsClient = new TLSClient(settings, callback);
        tlsClient.connect(settings.getHost(), settings.getServerPort());
    }

    @Override
    public SecureServer listen(IDSCPv2Settings settings, IDSCPv2Callback callback) {
        TLSServer tlsServer = new TLSServer(settings, callback);
        tlsServer.start();
        return tlsServer;
    }
}
