package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A Configuration class for the DefaultDapsDriver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class DefaultDapsDriverConfig {

    @NonNull
    private String dapsUrl = "https://daps.aisec.fraunhofer.de";
    @NonNull
    private Path keyStorePath = Paths.get("DUMMY-FILENAME.p12");
    @NonNull
    private char[] keyStorePassword = "password".toCharArray();
    @NonNull
    private String keyAlias = "1";
    @NonNull
    private char[] keyPassword = "password".toCharArray();
    @NonNull
    private Path trustStorePath = Paths.get("DUMMY-FILENAME.p12");
    @NonNull
    private char[] trustStorePassword = "password".toCharArray();

    public static class Builder {
        @NonNull
        private final DefaultDapsDriverConfig config = new DefaultDapsDriverConfig();

        @NonNull
        public Builder setDapsUrl(String dapsUrl) {
            this.config.dapsUrl = dapsUrl;
            return this;
        }

        @NonNull
        public Builder setKeyStorePath(Path path) {
            this.config.keyStorePath = path;
            return this;
        }

        @NonNull
        public Builder setKeyStorePassword(char[] password) {
            this.config.keyStorePassword = password;
            return this;
        }

        @NonNull
        public Builder setKeyAlias(String alias) {
            this.config.keyAlias = alias;
            return this;
        }

        @NonNull
        public Builder setKeyPassword(char[] password) {
            this.config.keyPassword = password;
            return this;
        }

        @NonNull
        public Builder setTrustStorePath(Path path) {
            this.config.trustStorePath = path;
            return this;
        }

        @NonNull
        public Builder setTrustStorePassword(char[] password) {
            this.config.trustStorePassword = password;
            return this;
        }

        @NonNull
        public DefaultDapsDriverConfig build() {
            return config;
        }
    }

    @NonNull
    public String getDapsUrl() {
        return dapsUrl;
    }

    @NonNull
    public Path getKeyStorePath() {
        return keyStorePath;
    }

    @NonNull
    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    @NonNull
    public String getKeyAlias() {
        return keyAlias;
    }

    @NonNull
    public char[] getKeyPassword() {
        return keyPassword;
    }

    @NonNull
    public Path getTrustStorePath() {
        return trustStorePath;
    }

    @NonNull
    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }

}
