package de.fhg.aisec.ids.idscp2.idscp_core.fsm.fsmListeners

/**
 * An FSM Listener Interface for the SecureChannel driver implemented by the FSM to restrict FSM API to
 * the SecureChannel drivers class of the IDSCP2
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface ScFsmListener {

    /**
     * A method for providing IDSCP2 data from the secure channel to the FSM
     */
    fun onMessage(data: ByteArray)

    /**
     * A method for providing internal SC errors to the fsm
     */
    fun onError(t: Throwable)

    /**
     * A method for notifying the fsm about closure of the secure channel
     */
    fun onClose()
}