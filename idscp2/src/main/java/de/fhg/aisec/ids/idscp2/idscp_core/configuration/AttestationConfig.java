package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

import java.util.Arrays;

/**
 * Attestation configuration class, containing attestation suite for supported / expected
 * attestation types
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class AttestationConfig {
    public final static String[] DEFAULT_RAT_MECHANISMS = new String[] {"Dummy", "TPM2d"};
    private final String[] ratMechanisms;

    public AttestationConfig() {
        this.ratMechanisms = DEFAULT_RAT_MECHANISMS;
    }

    public String[] getRatMechanisms() {
        return ratMechanisms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttestationConfig that = (AttestationConfig) o;
        return Arrays.equals(ratMechanisms, that.ratMechanisms);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ratMechanisms);
    }
}
