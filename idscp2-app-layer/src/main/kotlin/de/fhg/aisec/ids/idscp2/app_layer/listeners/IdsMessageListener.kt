package de.fhg.aisec.ids.idscp2.app_layer.listeners

import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fraunhofer.iais.eis.Message

fun interface IdsMessageListener {
    fun onMessage(connection: AppLayerConnection, header: Message?, payload: ByteArray?)
}