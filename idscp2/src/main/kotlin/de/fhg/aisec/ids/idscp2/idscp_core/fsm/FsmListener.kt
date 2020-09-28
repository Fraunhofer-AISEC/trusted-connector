package de.fhg.aisec.ids.idscp2.idscp_core.fsm

/**
 * An FSM Listener Interface implemented by the FSM to restrict FSM API to the drivers and the
 * secure channel class of the IDSCP2
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface FsmListener {
    /*
     * A method for providing IDSCP2 data from the secure channel to the FSM
     */
    fun onMessage(data: ByteArray)

    /*
     * A method for providing RatProver messages from the RatProverDriver implementation to the FSM
     */
    fun onRatProverMessage(controlMessage: InternalControlMessage)
    fun onRatProverMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray)

    /*
     * A method for providing RatVerifier messages from the RatVerifierDriver implementation to the
     * FSM
     */
    fun onRatVerifierMessage(controlMessage: InternalControlMessage)
    fun onRatVerifierMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray)

    /*
     * A method for providing internal errors to the fsm
     */
    fun onError(t: Throwable)

    /*
     * A method for notifying the fsm about closure of the secure channel
     */
    fun onClose()
}