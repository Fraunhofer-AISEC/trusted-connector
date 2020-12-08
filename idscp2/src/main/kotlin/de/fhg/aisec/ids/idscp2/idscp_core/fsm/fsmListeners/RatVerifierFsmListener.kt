package de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage

/**
 * An FSM Listener Interface for the RatVerifier driver implemented by the FSM to restrict FSM API to
 * the RatVerifier drivers class of the IDSCP2
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface RatVerifierFsmListener {

    /**
     * A method for providing RatVerifier messages from the RatVerifierDriver implementation to the
     * FSM
     */
    fun onRatVerifierMessage(controlMessage: InternalControlMessage)
    fun onRatVerifierMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray)
}