package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d;

import java.security.cert.Certificate;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Tpm2dProverConfig {

  private Certificate remoteCertificate;
  @NonNull private String tpm2dHost;

  private Tpm2dProverConfig() {
    tpm2dHost = System.getenv("TPM_HOST") != null ?
        System.getenv("TPM_HOST") : "localhost";
  }

  public static class Builder {
    private static Tpm2dProverConfig config = new Tpm2dProverConfig();

    @NonNull
    public Builder setRemoteCertificate(Certificate remoteCert) {
      config.remoteCertificate = remoteCert;
      return this;
    }

    @NonNull
    public Builder setTpmHost(String host) {
      config.tpm2dHost = host;
      return this;
    }

    public Tpm2dProverConfig build() {
      return config;
    }
  }

  public Certificate getRemoteCertificate() {
    return remoteCertificate;
  }

  @NonNull
  public String getTpm2dHost() {
    return tpm2dHost;
  }
}
