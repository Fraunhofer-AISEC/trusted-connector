package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import org.checkerframework.checker.units.qual.A;

/**
 * IDSCPv2 configuration class, containing information about keyStore and TrustStores, Attestation Types, host, ...
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IDSCPv2Settings {
    public static final int DEFAULT_SERVER_PORT = 8080;

    private int serverPort = DEFAULT_SERVER_PORT;
    private String host = "localhost";
    private String trustStorePath = null;
    private String trustStorePassword = "password";
    private String keyStorePath = null;
    private String keyStorePassword = "password";
    private String certAlias = "1.0.1";
    private String keyStoreKeyType = "RSA";
    private AttestationConfig supportedAttestation = new AttestationConfig();
    private AttestationConfig expectedAttestation = new AttestationConfig();

    public AttestationConfig getExpectedAttestation() {
        return expectedAttestation;
    }

    public AttestationConfig getSupportedAttestation() {
        return supportedAttestation;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStoreKeyType() {
        return keyStoreKeyType;
    }

    public String getCertAlias() {
        return certAlias;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    public void setKeyStoreKeyType(String keyStoreKeyType) {
        this.keyStoreKeyType = keyStoreKeyType;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
