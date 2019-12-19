package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;

public interface IDSCPv2Callback {
    void secureChannelConnectHandler(SecureChannel secureChannel);
    void secureChannelListenHandler(SecureChannel secureChannel);
    void connectionClosedHandler(String connectionId);
}
