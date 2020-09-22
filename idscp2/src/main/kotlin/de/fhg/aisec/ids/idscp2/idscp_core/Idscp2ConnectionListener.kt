package de.fhg.aisec.ids.idscp2.idscp_core

/**
 * An IDSCP Connection Listener interface that is implemented by the IDSCP2 Server to notify the
 * server about lifetimes of IDSCP connections. The server caches all active connections.
 */
interface Idscp2ConnectionListener {
    fun onError(t: Throwable)
    fun onClose(connection: Idscp2Connection)
}