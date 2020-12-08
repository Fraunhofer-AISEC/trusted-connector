package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.ARTIFACT_URI_PROPERTY
import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

class ArtifactRequestCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }
        val requestMessage = ArtifactRequestMessageBuilder().run {
            Utils.initMessageBuilder(this)
            exchange.getProperty(ARTIFACT_URI_PROPERTY)?.let {
                if (it is URI) {
                    it
                } else {
                    URI.create(it.toString())
                }
            }?.let {
                _requestedArtifact_(it)
            }
            build()
        }

        requestMessage.let {
            if (LOG.isDebugEnabled) {
                LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it))
            }
            exchange.message.setHeader(IDSCP2_HEADER, it)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactRequestCreationProcessor::class.java)
    }

}