package de.fhg.aisec.ids.idscp2.app_layer.listeners

import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection

fun interface GenericMessageListener {
    fun onMessage(connection: AppLayerConnection, type: String, data: ByteArray)
}