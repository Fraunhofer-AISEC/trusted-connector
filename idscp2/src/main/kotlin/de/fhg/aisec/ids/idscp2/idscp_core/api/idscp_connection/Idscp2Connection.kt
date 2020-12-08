package de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection

import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2TimeoutException
import de.fhg.aisec.ids.idscp2.idscp_core.error.Idscp2WouldBlockException
import kotlin.jvm.Throws

/**
 * The IDSCP2 Connection class holds connections between connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
interface Idscp2Connection {
    val id: String

    /**
     * Unlock messaging when a message listener is registered, to avoid race conditions
     * and messages loss
     */
    fun unlockMessaging()

    /**
     * Close the idscp connection
     */
    @Throws(Idscp2Exception::class)
    fun close()

    /**
     * Send data to the peer IDSCP2 connector without timeout and retry interval when
     * connection is currently not available
     */
    @Throws(Idscp2Exception::class, Idscp2WouldBlockException::class)
    fun nonBlockingSend(msg: ByteArray)

    /**
     * Send data to the peer IDSCP2 connector and block until done
     */
    @Throws(Idscp2Exception::class, Idscp2TimeoutException::class)
    fun blockingSend(msg: ByteArray, timeout: Long, retryInterval: Long = 0)

    /**
     * Repeat remote attestation verification of remote peer
     */
    @Throws(Idscp2Exception::class)
    fun repeatRat()

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