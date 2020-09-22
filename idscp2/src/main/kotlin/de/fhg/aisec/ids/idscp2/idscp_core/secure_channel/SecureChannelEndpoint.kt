package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel

/**
 * An interface for a secureChannelEndpoint e.g. TLS Client and TLS Server Thread
 * Used to delegate functions and messages between secure channel and its endpoints
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureChannelEndpoint {
    /*
     * API to close the secure channel endpoint
     */
    fun close()

    /*
     * API to delegate messages from an input listener to the secure channel endpoint
     */
    fun onMessage(bytes: ByteArray)

    /*
     * Send data from the secure channel endpoint to the peer connector
     *
     * return true when data has been sent, else false
     */
    fun send(bytes: ByteArray): Boolean

    /*
     * check if the endpoint is connected
     */
    val isConnected: Boolean
}