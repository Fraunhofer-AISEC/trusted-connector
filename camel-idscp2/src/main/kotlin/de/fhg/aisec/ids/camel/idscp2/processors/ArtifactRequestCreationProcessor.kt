package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder
import de.fraunhofer.iais.eis.ids.jsonld.Serializer
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

class ArtifactRequestCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        val requestMessageBuilder = ArtifactRequestMessageBuilder()
        Idscp2OsgiComponent.infoModelManager.initMessageBuilder(requestMessageBuilder)
        val artifactUriProperty = exchange.getProperty(ARTIFACT_URI_PROPERTY)
        val artifactUri = if (artifactUriProperty is URI) {
            artifactUriProperty
        } else {
            URI.create(artifactUriProperty.toString())
        }
        requestMessageBuilder._requestedArtifact_(artifactUri)
        val requestMessageSerialization = SERIALIZER.serialize(requestMessageBuilder.build())
        if (LOG.isDebugEnabled) {
            LOG.debug("ArtifactRequestMessage serialization: {}", requestMessageSerialization)
        }
        exchange.`in`.setHeader("idscp2-header", requestMessageSerialization)
    }

    companion object {
        const val ARTIFACT_URI_PROPERTY = "artifactUri"

        private val LOG = LoggerFactory.getLogger(ArtifactRequestCreationProcessor::class.java)
        private val SERIALIZER: Serializer by lazy { Serializer() }
    }

}