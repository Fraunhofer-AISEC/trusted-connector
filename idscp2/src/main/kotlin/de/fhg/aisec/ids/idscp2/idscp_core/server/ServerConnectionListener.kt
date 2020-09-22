package de.fhg.aisec.ids.idscp2.idscp_core.server

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection

interface ServerConnectionListener {
    fun onConnectionCreated(connection: Idscp2Connection)
    fun onConnectionClose(connection: Idscp2Connection?)
}