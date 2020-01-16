package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverInstance;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

/**
 * Remote Attestation Prover Driver implementation class for TPM2.0
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TPM2ProverImpl implements RatProverDriver {

    @Override
    public RatProverInstance start(FsmListener fsmListener) {
        TPM2Prover newProver = new TPM2Prover(fsmListener);
        newProver.start();
        return newProver;
    }
}
