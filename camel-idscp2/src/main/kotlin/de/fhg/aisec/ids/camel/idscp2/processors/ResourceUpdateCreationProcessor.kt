package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants
import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.ProviderDB
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ResourceUpdateMessageBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI


class ResourceUpdateCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val artifactUri = exchange.getProperty(Constants.ARTIFACT_URI_PROPERTY)?.let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }

        val usedContract = ProviderDB.artifactUrisMapped2ContractAgreements[artifactUri]
            ?: throw RuntimeException("No UC contract found for resource/artifact $artifactUri")
        if (LOG.isDebugEnabled) {
            LOG.debug("Contract for requested Artifact found {}", usedContract)
        }

        ResourceUpdateMessageBuilder().run {
            Utils.initMessageBuilder(this)
            _affectedResource_(artifactUri)
            _transferContract_(usedContract)
            build().let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it))
                }
                exchange.message.setHeader(IDSCP2_HEADER, it)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ResourceUpdateCreationProcessor::class.java)
    }

}