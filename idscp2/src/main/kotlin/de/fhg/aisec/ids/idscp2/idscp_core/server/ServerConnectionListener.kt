package de.fhg.aisec.ids.idscp2.idscp_core.server

import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection

interface ServerConnectionListener<CC: Idscp2Connection> {
    fun onConnectionCreated(connection: CC)
    fun onConnectionClose(connection: CC)
}