package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Constants.IDS_TYPE
import de.fraunhofer.iais.eis.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class IdsMessageTypeExtractionProcessor : Processor {
    override fun process(exchange: Exchange) {
        processHeader(exchange)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IdsMessageTypeExtractionProcessor::class.java)

        fun processHeader(exchange: Exchange) {
            if (LOG.isDebugEnabled) {
                LOG.debug("[IN] ${IdsMessageTypeExtractionProcessor::class.java.simpleName}")
            }
            exchange.message.getHeader(IDSCP2_HEADER, Message::class.java)?.let { header ->
                val messageType = when (header) {
                    is ArtifactRequestMessage -> ArtifactRequestMessage::class.simpleName
                    is ArtifactResponseMessage -> ArtifactResponseMessage::class.simpleName
                    is ContractRequestMessage -> ContractRequestMessage::class.simpleName
                    is ContractResponseMessage -> ContractResponseMessage::class.simpleName
                    is ContractAgreementMessage -> ContractAgreementMessage::class.simpleName
                    is ContractRejectionMessage -> ContractRejectionMessage::class.simpleName
                    is RejectionMessage -> RejectionMessage::class.simpleName
                    else -> header::class.simpleName
                }
                if (LOG.isDebugEnabled) {
                    LOG.debug("Detected ids-type: {}", messageType)
                }
                exchange.setProperty(IDS_TYPE, messageType)
            }
        }
    }
}
