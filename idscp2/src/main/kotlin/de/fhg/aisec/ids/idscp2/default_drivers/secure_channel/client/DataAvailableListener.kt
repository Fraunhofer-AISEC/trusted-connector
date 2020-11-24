package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.client

/**
 * An interface for DataAvailableListeners, that will be notified when new data has been received
 * at the sslSocket
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface DataAvailableListener {
    /*
     * Provide incoming data to listener
     */
    fun onMessage(bytes: ByteArray)

    /*
     * Notify listener that an error has occurred
     */
    fun onError(e: Throwable)

    /*
     * Notify listener that the socket has been closed
     */
    fun onClose()
}