package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ArtifactResponseMessage
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class ArtifactResponseProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val artifactResponseMessage = exchange.message.getHeader(
                Constants.IDSCP2_HEADER, ArtifactResponseMessage::class.java)
        if (LOG.isDebugEnabled) {
            LOG.debug("Received serialization header: {}", SERIALIZER.serialize(artifactResponseMessage))
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("Received serialization body: {}", exchange.message.body)
        }

        // add behavior here depending on existing artifact or not
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactResponseProcessor::class.java)
    }

}