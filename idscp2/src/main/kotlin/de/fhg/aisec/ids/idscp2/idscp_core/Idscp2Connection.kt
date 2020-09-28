package de.fhg.aisec.ids.idscp2.idscp_core

/**
 * The IDSCP2 Connection class holds connections between connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
interface Idscp2Connection {
    val id: String

    fun unlockMessaging()

    /**
     * Close the idscp connection
     */
    fun close()

    /**
     * Send data to the peer IDSCP2 connector
     */
    fun send(msg: ByteArray)

    fun onMessage(msg: ByteArray)

    fun onError(t: Throwable)

    fun onClose()

    /**
     * Check if the idscp connection is currently established
     *
     * @return Connection established state
     */
    val isConnected: Boolean

    fun addConnectionListener(listener: Idscp2ConnectionListener)

    fun removeConnectionListener(listener: Idscp2ConnectionListener): Boolean

    fun addMessageListener(listener: Idscp2MessageListener)

    fun removeMessageListener(listener: Idscp2MessageListener): Boolean
}