package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d;

import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation.IdsAttestationType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;

/**
 * A configuration class for TPM2d RatVerifier Driver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TPM2dVerifierConfig {

    private Certificate localCertificate;
    @NonNull
    private URI ttpUri;
    @NonNull
    private IdsAttestationType expectedAType = IdsAttestationType.BASIC;
    private int expectedAttestationMask = 0;

    private TPM2dVerifierConfig() {
        try {
            ttpUri = new URI("https://invalid-ttp-uri/rat-verify");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder {
        private static final TPM2dVerifierConfig config = new TPM2dVerifierConfig();

        @NonNull
        public Builder setTtpUri(URI ttpUri) {
            config.ttpUri = ttpUri;
            return this;
        }

        @NonNull
        public Builder setLocalCertificate(Certificate localCert) {
            config.localCertificate = localCert;
            return this;
        }

        @NonNull
        public Builder setExpectedAttestationType(IdsAttestationType aType) {
            config.expectedAType = aType;
            return this;
        }

        public Builder setExpectedAttestationMask(int mask) {
            config.expectedAttestationMask = mask;
            return this;
        }

        public TPM2dVerifierConfig build() {
            return config;
        }
    }

    @NonNull
    public URI getTtpUri() {
        return ttpUri;
    }

    @NonNull
    public IdsAttestationType getExpectedAType() {
        return expectedAType;
    }

    public Certificate getLocalCertificate() {
        return localCertificate;
    }

    public int getExpectedAttestationMask() {
        return expectedAttestationMask;
    }
}
