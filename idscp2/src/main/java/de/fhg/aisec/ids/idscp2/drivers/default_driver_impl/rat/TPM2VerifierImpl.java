package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierInstance;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

/**
 * Remote Attestation Verifier Driver implementation class for TPM2.0
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TPM2VerifierImpl implements RatVerifierDriver {

    @Override
    public RatVerifierInstance start(String instance, FsmListener fsmListener) {
        TPM2Verifier newVerifier = new TPM2Verifier(fsmListener);
        newVerifier.start();
        return newVerifier;
    }
}
