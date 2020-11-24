package de.fhg.aisec.ids.idscp2.app_layer

import com.google.protobuf.ByteString
import de.fhg.aisec.ids.idscp2.app_layer.listeners.GenericMessageListener
import de.fhg.aisec.ids.idscp2.app_layer.messages.AppLayer
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2ConnectionImpl
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2MessageListener
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.Idscp2Configuration
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import org.slf4j.LoggerFactory
import java.util.*

class AppLayerConnection private constructor(private val idscp2Connection: Idscp2Connection) :
        Idscp2Connection by idscp2Connection {
    private var idscp2MessageListener: Idscp2MessageListener? = null
    private val genericMessageListeners: MutableSet<GenericMessageListener> = Collections.synchronizedSet(HashSet())

    constructor(secureChannel: SecureChannel, configuration: Idscp2Configuration, dapsDriver: DapsDriver):
        this(Idscp2ConnectionImpl(secureChannel, configuration, dapsDriver))

    private fun assureMessageListener() {
        if (idscp2MessageListener == null) {
            val listener = Idscp2MessageListener { _, data ->
                try {
                    val appLayerMessage = AppLayer.AppLayerMessage.parseFrom(data)
                    if (LOG.isTraceEnabled) {
                        LOG.trace("Received AppLayerMessage ${appLayerMessage.messageCase}")
                    }
                    when (appLayerMessage.messageCase) {
                        AppLayer.AppLayerMessage.MessageCase.GENERICMESSAGE -> {
                            genericMessageListeners.forEach {
                                val genericMessage = appLayerMessage.genericMessage
                                it.onMessage(this,
                                        genericMessage.header,
                                        genericMessage.payload?.toByteArray())
                            }
                        }
                        else -> LOG.warn("Unknown app layer message header encountered.")
                    }
                } catch (e: Exception) {
                    LOG.error("Error processing AppLayerMessage", e)
                }
            }
            idscp2Connection.addMessageListener(listener)
            idscp2MessageListener = listener
        }
    }

    fun sendGenericMessage(header: String?, payload: ByteArray?) {
        val message = AppLayer.AppLayerMessage.newBuilder()
                .setGenericMessage(AppLayer.GenericMessage.newBuilder()
                        .also {
                            if (header != null) {
                                it.header = header
                            }
                            if (payload != null) {
                                it.payload = ByteString.copyFrom(payload)
                            }
                        }
                        .build())
                .build()
        idscp2Connection.send(message.toByteArray())
    }

    fun addGenericMessageListener(listener: GenericMessageListener) {
        assureMessageListener()
        genericMessageListeners += listener
        if (LOG.isTraceEnabled) {
            LOG.trace("Added GenericMessageListener $listener for connection {}", idscp2Connection.id)
        }
    }

    fun removeGenericMessageListener(listener: GenericMessageListener): Boolean {
        val ret = genericMessageListeners.remove(listener)
        if (genericMessageListeners.isEmpty()) {
            idscp2MessageListener?.let { idscp2Connection.removeMessageListener(it) }
            idscp2MessageListener = null
        }
        return ret
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AppLayerConnection::class.java)
        private val appLayerConnections = Collections.synchronizedMap(
                WeakHashMap<Idscp2Connection, AppLayerConnection>())

        fun from(idscp2Connection: Idscp2Connection): AppLayerConnection {
            return if (idscp2Connection is AppLayerConnection) {
                idscp2Connection
            } else {
                appLayerConnections.computeIfAbsent(idscp2Connection) { AppLayerConnection(it) }
            }
        }
    }

}