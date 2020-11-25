package de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.InternalControlMessage

/**
 * An FSM Listener Interface for the RatProver driver implemented by the FSM to restrict FSM API to
 * the RatProver drivers class of the IDSCP2
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface RatProverFsmListener {

    /**
     * A method for providing RatProver messages from the RatProverDriver implementation to the FSM
     */
    fun onRatProverMessage(controlMessage: InternalControlMessage)
    fun onRatProverMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray)
}