package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps;

import org.checkerframework.checker.nullness.qual.NonNull;

public class DefaultDapsDriverConfig {

  @NonNull private String dapsUrl = "https://daps.aisec.fraunhofer.de";
  @NonNull private String keyStorePath = "";
  @NonNull private String keyStorePassword = "password";
  @NonNull private String keyAlias = "1";
  @NonNull private String trustStorePath = "";
  @NonNull private String trustStorePassword = "password";
  @NonNull private String connectorUUID = "";

  public static class Builder {
    @NonNull private DefaultDapsDriverConfig config = new DefaultDapsDriverConfig();

    @NonNull
    public Builder setDapsUrl(String dapsUrl) {
      this.config.dapsUrl = dapsUrl;
      return this;
    }

    @NonNull
    public Builder setKeyStorePath(String path) {
      this.config.keyStorePath = path;
      return this;
    }

    @NonNull
    public Builder setKeyStorePassword(String password) {
      this.config.keyStorePassword = password;
      return this;
    }

    @NonNull
    public Builder setKeyAlias(String alias) {
      this.config.keyAlias = alias;
      return this;
    }

    @NonNull
    public Builder setTrustStorePath(String path) {
      this.config.trustStorePath = path;
      return this;
    }

    @NonNull
    public Builder setTrustStorePassword(String password) {
      this.config.trustStorePassword = password;
      return this;
    }

    @NonNull
    public Builder setConnectorUUID(String uuid) {
      this.config.connectorUUID = uuid;
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
  public String getKeyStorePath() {
    return keyStorePath;
  }

  @NonNull
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  @NonNull
  public String getKeyAlias() {
    return keyAlias;
  }

  @NonNull
  public String getTrustStorePath() {
    return trustStorePath;
  }

  @NonNull
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  @NonNull
  public String getConnectorUUID() {
    return connectorUUID;
  }
}
