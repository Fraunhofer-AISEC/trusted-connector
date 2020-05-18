package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.security.cert.Certificate;

/**
 * A configuration class for TPM2d RatPriver driver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TPM2dProverConfig {

  private Certificate remoteCertificate;
  @NonNull private String tpm2dHost;

  private TPM2dProverConfig() {
    tpm2dHost = System.getenv("TPM_HOST") != null ?
        System.getenv("TPM_HOST") : "localhost";
  }

  public static class Builder {
    private static final TPM2dProverConfig config = new TPM2dProverConfig();

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

    public TPM2dProverConfig build() {
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
