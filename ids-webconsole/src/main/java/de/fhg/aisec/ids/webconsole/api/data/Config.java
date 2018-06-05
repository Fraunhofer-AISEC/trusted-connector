package de.fhg.aisec.ids.webconsole.api.data;

import de.fhg.aisec.ids.api.settings.ConnectorConfig;

public class Config implements ConnectorConfig {
    private String brokerUrl;
    private String ttpHost;
    private int ttpPort;
    private String acmeServerWebcon;
    private String acmeDnsWebcon;
    private int acmePortWebcon;

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getTtpHost() {
        return ttpHost;
    }

    public int getTtpPort() {
        return ttpPort;
    }

    public String getAcmeServerWebcon() {
        return acmeServerWebcon;
    }

    public String getAcmeDnsWebcon() {
        return acmeDnsWebcon;
    }

    public int getAcmePortWebcon() {
        return acmePortWebcon;
    }
}
