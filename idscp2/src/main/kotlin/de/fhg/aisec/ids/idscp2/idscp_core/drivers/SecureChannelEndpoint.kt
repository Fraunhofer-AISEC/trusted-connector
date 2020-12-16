package de.fhg.aisec.ids.idscp2.idscp_core.drivers

/**
 * An interface for a secureChannelEndpoint e.g. TLS Client and TLS Server Thread
 * Used to delegate functions and messages between secure channel and its endpoints
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureChannelEndpoint {

    /**
     * API to close the secure channel endpoint
     */
    fun close()

    /**
     * Send data from the secure channel endpoint to the peer connector
     *
     * ATTENTION: The developer must ensure not to trigger the FSM by another event from the thread
     * that actually executes the SecureChannelEndpoint.send(bytes) method, but must simply return
     * true or false, regarding the success of this method. The issue which occurs by using the thread
     * of the send() method is that the FSM is blocked within a transition until send() returns
     * and the the following state of the FSM depends on the return value. When this thread would trigger
     * the FSM again within the current transition then first the new transition would be executed
     * but than it would be overwritten by the current one. To avoid this case of misuse, the FSM
     * will throw a Runtime Exception to let the driver developer know, that this is not a good idea.
     * For more information, see the checkForFsmCycles() method within the FSM
     *
     * return true when data has been sent, else false
     */
    fun send(bytes: ByteArray): Boolean

    /**
     * check if the endpoint is connected
     */
    val isConnected: Boolean
}