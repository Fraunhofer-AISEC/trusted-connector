package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;

/**
 * An interface for the remote attestation prover driver that is used for proving the current state by RAT mechanism
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface RatProverDriver {
    RatProverInstance start(String instance, FsmListener fsmListener);
}
