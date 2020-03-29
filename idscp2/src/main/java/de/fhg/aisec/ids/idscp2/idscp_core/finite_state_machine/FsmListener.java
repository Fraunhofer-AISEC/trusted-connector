package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.messages.IDSCPv2.*;

/**
 * An FSM Listener Interface implemented by the FSM to restrict FSM API to the drivers and the
 * secure channel class of the IDSCPv2
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface FsmListener {

    /*
     * A method for providing IDSCPv2 data from the secure channel to the FSM
     */
    void onMessage(byte[] data);

    /*
     * A method for providing RatProver messages from the RatProverDriver implementation to the FSM
     */
    void onRatProverMessage(InternalControlMessage controlMessage, IdscpMessage idscpMessage);

    /*
     * A method for providing RatVerifier messages from the RatVerifierDriver implementation to the
     * FSM
     */
    void onRatVerifierMessage(InternalControlMessage controlMessage, IdscpMessage idscpMessage);

    /*
     * A method for providing internal errors to the fsm
     */
    void onError();

    /*
     * A method for notifying the fsm about closure of the secure channel
     */
    void onClose();
}
