package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * IDSCP2 configuration class, contains information about keyStore and TrustStores,
 * Attestation Types, host, DAPS, ...
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2Settings {
    public static final int DEFAULT_SERVER_PORT = 29292;

    private int serverPort = DEFAULT_SERVER_PORT;
    private String host = "localhost";
    private String trustStorePath = null;
    private String trustStorePassword = "password";
    private String keyStorePath = null;
    private String keyStorePassword = "password";
    private String certificateAlias = "1.0.1";
    private String dapsKeyAlias = "1";
    private String keyStoreKeyType = "RSA";
    private AttestationConfig supportedAttestation = new AttestationConfig();
    private AttestationConfig expectedAttestation = new AttestationConfig();
    private int ratTimeoutDelay = 20;

    @SuppressWarnings("unused")
    public static class Builder {
        @NonNull
        private final Idscp2Settings settings = new Idscp2Settings();

        @NonNull
        public Builder setHost(String host) {
            this.settings.host = host;
            return this;
        }

        @NonNull
        public Builder setServerPort(int serverPort) {
            this.settings.serverPort = serverPort;
            return this;
        }

        @NonNull
        public Builder setTrustStorePath(String path) {
            this.settings.trustStorePath = path;
            return this;
        }

        @NonNull
        public Builder setKeyStorePath(String path) {
            this.settings.keyStorePath = path;
            return this;
        }

        @NonNull
        public Builder setTrustStorePassword(String pwd) {
            this.settings.trustStorePassword = pwd;
            return this;
        }

        @NonNull
        public Builder setKeyStorePassword(String pwd) {
            this.settings.keyStorePassword = pwd;
            return this;
        }

        @NonNull
        public Builder setCertificateAlias(String alias) {
            this.settings.certificateAlias = alias;
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
        public Builder setSupportedAttestation(AttestationConfig suite) {
            this.settings.supportedAttestation = suite;
            return this;
        }

        @NonNull
        public Builder setExpectedAttestation(AttestationConfig suite) {
            this.settings.expectedAttestation = suite;
            return this;
        }

        public Builder setRatTimeoutDelay(int delay) {
            this.settings.ratTimeoutDelay = delay;
            return this;
        }

        @NonNull
        public Idscp2Settings build() {
            return this.settings;
        }

    }

    public int getServerPort() {
        return serverPort;
    }

    public String getHost() {
        return host;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getCertificateAlias() {
        return certificateAlias;
    }

    public String getDapsKeyAlias() {
        return dapsKeyAlias;
    }

    public String getKeyStoreKeyType() {
        return keyStoreKeyType;
    }

    public AttestationConfig getSupportedAttestation() {
        return supportedAttestation;
    }

    public AttestationConfig getExpectedAttestation() {
        return expectedAttestation;
    }

    public int getRatTimeoutDelay() {
        return ratTimeoutDelay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Idscp2Settings that = (Idscp2Settings) o;
        return serverPort == that.serverPort &&
                ratTimeoutDelay == that.ratTimeoutDelay &&
                Objects.equals(host, that.host) &&
                Objects.equals(trustStorePath, that.trustStorePath) &&
                Objects.equals(trustStorePassword, that.trustStorePassword) &&
                Objects.equals(keyStorePath, that.keyStorePath) &&
                Objects.equals(keyStorePassword, that.keyStorePassword) &&
                Objects.equals(certificateAlias, that.certificateAlias) &&
                Objects.equals(dapsKeyAlias, that.dapsKeyAlias) &&
                Objects.equals(keyStoreKeyType, that.keyStoreKeyType) &&
                Objects.equals(supportedAttestation, that.supportedAttestation) &&
                Objects.equals(expectedAttestation, that.expectedAttestation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverPort, host, trustStorePath, trustStorePassword, keyStorePath,
                keyStorePassword, certificateAlias, dapsKeyAlias, keyStoreKeyType, supportedAttestation,
                expectedAttestation, ratTimeoutDelay);
    }
}
