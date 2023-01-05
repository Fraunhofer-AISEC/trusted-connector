package de.fhg.aisec.ids.camel.processors

import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.camel.processors.Constants.IDS_HEADER
import de.fraunhofer.iais.eis.DescriptionRequestMessage
import de.fraunhofer.iais.eis.DescriptionResponseMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("descriptionRequestProcessor")
class DescriptionRequestProcessor: Processor {

    @Autowired
    private lateinit var infoModel: InfoModel

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val descriptionRequestMessage = exchange.message.getHeader(IDS_HEADER, DescriptionRequestMessage::class.java)

        DescriptionResponseMessageBuilder()
            ._correlationMessage_(descriptionRequestMessage.id)
            .let { exchange.message.setHeader(IDS_HEADER, it) }

        exchange.message.body = infoModel.connectorAsJsonLd
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DescriptionRequestProcessor::class.java)
    }
}
