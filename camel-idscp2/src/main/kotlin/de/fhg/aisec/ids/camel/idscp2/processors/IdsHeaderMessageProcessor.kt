package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Constants.IDS_TYPE
import de.fhg.aisec.ids.camel.idscp2.processors.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ArtifactRequestMessage
import de.fraunhofer.iais.eis.Message
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class IdsHeaderMessageProcessor : Processor {
    override fun process(exchange: Exchange) {
        val header = exchange.message.getHeader(IDSCP2_HEADER).toString()
        val headerMessage = SERIALIZER.deserialize(header, Message::class.java)
        exchange.setProperty(IDSCP2_HEADER, headerMessage)
        val messageType = when (headerMessage) {
            is ArtifactRequestMessage -> ArtifactRequestMessage::class.simpleName
            else -> headerMessage::class.simpleName
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Detected ids-type: {}", messageType)
        }
        exchange.setProperty(IDS_TYPE, messageType)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactRequestCreationProcessor::class.java)
    }
}