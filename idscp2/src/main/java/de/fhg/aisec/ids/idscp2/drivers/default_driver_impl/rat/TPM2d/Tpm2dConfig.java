package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.TPM2d;

import de.fhg.aisec.ids.messages.Tpm2dAttestation.IdsAttestationType;
import java.net.URI;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Tpm2dConfig {

  private int attestationMask = 0;
  @NonNull private IdsAttestationType expectedAType = IdsAttestationType.BASIC;
  @NonNull private URI ttpUri = URI.create("");

  public static class Builder {
    @NonNull private Tpm2dConfig config = new Tpm2dConfig();

    @NonNull
    public Builder setAttestationMask(int attestationMask) {
      config.attestationMask = attestationMask;
      return this;
    }

    @NonNull
    public Builder setIdsAttestationType(IdsAttestationType aType) {
      config.expectedAType = aType;
      return this;
    }

    @NonNull
    public Builder setTtpUri(URI ttp) {
      config.ttpUri = ttp;
      return this;
    }

    @NonNull
    public Tpm2dConfig build() {
      return config;
    }
  }

  public IdsAttestationType getExpectedAType() {
    return expectedAType;
  }

  public int getAttestationMask() {
    return attestationMask;
  }

  public URI getTtpUri() {
    return ttpUri;
  }
}
