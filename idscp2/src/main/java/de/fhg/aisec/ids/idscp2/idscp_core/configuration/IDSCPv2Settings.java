package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
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
    private String dapsKeyAlias = "1";
    private String keyStoreKeyType = "RSA";
    private AttestationConfig supportedAttestation = new AttestationConfig();
    private AttestationConfig expectedAttestation = new AttestationConfig();

    public static class Builder {
        @NonNull
        private IDSCPv2Settings settings = new IDSCPv2Settings();

        @NonNull
        public Builder setHost(String host) {
            this.settings.host = host;
            return this;
        }

        @NonNull
        public Builder setTrustStore(String path) {
            this.settings.trustStorePath = path;
            return this;
        }

        @NonNull
        public Builder setKeyStore(String path) {
            this.settings.keyStorePath = path;
            return this;
        }

        @NonNull
        public Builder setTrustStorePwd(String pwd) {
            this.settings.trustStorePassword = pwd;
            return this;
        }

        @NonNull
        public Builder setKeyStorePwd(String pwd) {
            this.settings.keyStorePassword = pwd;
            return this;
        }

        @NonNull
        public Builder setCertificateAlias(String alias) {
            this.settings.certAlias = alias;
            return this;
        }

        @NonNull
        public Builder setDapsKeyAlias(String alias) {
            this.settings.dapsKeyAlias = alias;
            return this;
        }

        @NonNull
        public Builder setKeyStoreKeyType(String keyType) {
            this.settings.keyStoreKeyType = keyType;
            return this;
        }

        @NonNull
        public Builder setSupportedAttestationSuite(AttestationConfig suite) {
            this.settings.supportedAttestation = suite;
            return this;
        }

        @NonNull
        public Builder setExpectedAttestationSuite(AttestationConfig suite) {
            this.settings.expectedAttestation = suite;
            return this;
        }

        @NonNull
        public IDSCPv2Settings build() {
            return this.settings;
        }

    }

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

    public String getDapsKeyAlias() {
        return dapsKeyAlias;
    }

    public String getHost() {
        return host;
    }
}
