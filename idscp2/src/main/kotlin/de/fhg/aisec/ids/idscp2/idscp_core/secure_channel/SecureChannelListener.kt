package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel

/**
 * An interface for a secure channel listener, implemented by the secure channel
 */
interface SecureChannelListener {
    /*
     * Delegate data from secure channel endpoint to the secure channel
     */
    fun onMessage(data: ByteArray)

    /*
     * Delegate an error from an secure channel endpoint to the secure channel
     */
    fun onError(t: Throwable)

    /*
     * Notify secure channel that secure channel endpoint has been closed
     */
    fun onClose()
}