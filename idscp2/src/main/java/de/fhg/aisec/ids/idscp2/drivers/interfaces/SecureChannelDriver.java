package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Callback;
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.IDSCPv2Settings;

public interface SecureChannelDriver {
    void connect(IDSCPv2Settings settings, IDSCPv2Callback callback);
    SecureServer listen(IDSCPv2Settings settings, IDSCPv2Callback callback);
}
