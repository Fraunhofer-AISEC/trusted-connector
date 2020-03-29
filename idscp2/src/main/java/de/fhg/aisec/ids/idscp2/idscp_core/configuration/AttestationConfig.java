package de.fhg.aisec.ids.idscp2.idscp_core.configuration;

/**
 * Attestation configuration class, containing attestation suite for supported / expected
 * attestation types
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class AttestationConfig {

    public AttestationConfig(){

    }

    public String[] getRatMechanisms(){
        return new String[] {"Dummy", "TPM2d"};
    }

}
