package de.fhg.aisec.ids.idscp2.app_layer

import com.google.protobuf.ByteString
import de.fhg.aisec.ids.idscp2.app_layer.listeners.GenericMessageListener
import de.fhg.aisec.ids.idscp2.app_layer.listeners.IdsMessageListener
import de.fhg.aisec.ids.idscp2.app_layer.messages.AppLayer
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionImpl
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageListener
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fraunhofer.iais.eis.Message
import org.slf4j.LoggerFactory
import java.util.*

class AppLayerConnection private constructor(private val idscp2Connection: Idscp2Connection) :
        Idscp2Connection by idscp2Connection {
    private val idscp2MessageListener: Idscp2MessageListener = Idscp2MessageListener { _, data ->
        try {
            val appLayerMessage = AppLayer.AppLayerMessage.parseFrom(data)
            if (LOG.isTraceEnabled) {
                LOG.trace("Received AppLayerMessage ${appLayerMessage.messageCase}")
            }
            when (appLayerMessage.messageCase) {
                AppLayer.AppLayerMessage.MessageCase.GENERICMESSAGE -> {
                    genericMessageListeners.forEach { listener ->
                        val genericMessage = appLayerMessage.genericMessage
                        listener.onMessage(
                                this,
                                genericMessage.header,
                                genericMessage.payload?.toByteArray())
                    }
                }
                AppLayer.AppLayerMessage.MessageCase.IDSMESSAGE -> {
                    idsMessageListeners.forEach { listener ->
                        val idsMessage = appLayerMessage.idsMessage
                        listener.onMessage(
                                this,
                                idsMessage.header?.let {
                                    Utils.SERIALIZER.deserialize(it, Message::class.java)
                                },
                                idsMessage.payload?.toByteArray())
                    }
                }
                else -> LOG.warn("Unknown IDSCP2 app layer message type encountered.")
            }
        } catch (e: Exception) {
            LOG.error("Error processing AppLayerMessage", e)
        }
    }
    private val genericMessageListeners: MutableSet<GenericMessageListener> =
            Collections.synchronizedSet(LinkedHashSet())
    private val idsMessageListeners: MutableSet<IdsMessageListener> =
            Collections.synchronizedSet(LinkedHashSet())

    constructor(secureChannel: SecureChannel, settings: Idscp2Settings, dapsDriver: DapsDriver) :
            this(Idscp2ConnectionImpl(secureChannel, settings, dapsDriver)) {
        idscp2Connection.addMessageListener(idscp2MessageListener)
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
        genericMessageListeners += listener
        if (LOG.isTraceEnabled) {
            LOG.trace("Added GenericMessageListener $listener for connection {}", idscp2Connection.id)
        }
    }

    fun removeGenericMessageListener(listener: GenericMessageListener) = genericMessageListeners.remove(listener)

    fun sendIdsMessage(header: Message?, payload: ByteArray?) {
        val message = AppLayer.AppLayerMessage.newBuilder()
                .setIdsMessage(AppLayer.IdsMessage.newBuilder()
                        .also {
                            if (header != null) {
                                it.header = Utils.SERIALIZER.serialize(header)
                            }
                            if (payload != null) {
                                it.payload = ByteString.copyFrom(payload)
                            }
                        }
                        .build())
                .build()
        idscp2Connection.send(message.toByteArray())
    }

    fun addIdsMessageListener(listener: IdsMessageListener) {
        idsMessageListeners += listener
        if (LOG.isTraceEnabled) {
            LOG.trace("Added IdsMessageListener $listener for connection {}", idscp2Connection.id)
        }
    }

    fun removeIdsMessageListener(listener: IdsMessageListener) = idsMessageListeners.remove(listener)

    override fun toString(): String {
        return "AppLayerConnection($id)"
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