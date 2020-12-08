package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.ProviderDB
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger


class ArtifactRequestProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val artifactRequestMessage = exchange.message.getHeader(
                IDSCP2_HEADER, ArtifactRequestMessage::class.java)
        val requestedArtifact = artifactRequestMessage.requestedArtifact
//        val transferContract = artifactRequestMessage.transferContract

        // TODO: If transferContract doesn't match expected contract from database, send rejection!
        val usedContract = ProviderDB.artifactUrisMapped2ContractAgreements[requestedArtifact]
        if (LOG.isDebugEnabled) {
            LOG.debug("Contract for requested Artifact found {}", usedContract)
        }

        // if artifact is available/authorised create response else create rejection message
        if (!ProviderDB.availableArtifactURIs.containsKey(requestedArtifact)) {
            createRejectionMessage(exchange, artifactRequestMessage, RejectionReason.NOT_FOUND)
        } else if (!ProviderDB.contractAgreements.containsKey(usedContract)) {
            createRejectionMessage(exchange, artifactRequestMessage, RejectionReason.NOT_AUTHORIZED)
        } else {
            // Proceed normally and send ArtifactResponseMessage
            ArtifactResponseMessageBuilder().run {
                Utils.initMessageBuilder(this)
                _correlationMessage_(artifactRequestMessage.id)
                _transferContract_(usedContract)
                build().let {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it))
                    }
                    exchange.message.setHeader(IDSCP2_HEADER, it)
                }
            }

            // create sample artifact
            val artifactDate = Utils.createGregorianCalendarTimestamp(System.currentTimeMillis())
            val artifact = ArtifactBuilder()
                    ._byteSize_(BigInteger.valueOf(50000))
                    ._checkSum_("ABCDEFG-CHECKSUM")
                    ._creationDate_(artifactDate)
                    ._duration_(BigDecimal(5000))
                    ._fileName_("testArtifactFilename.dat")
                    .build()

            SERIALIZER.serialize(artifact).let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialisation body: {}", it)
                }
                exchange.message.body = it
            }
        }
    }

    private fun createRejectionMessage(exchange: Exchange, artifactRequestMessage: ArtifactRequestMessage,
                                       rejectionReason: RejectionReason) {
        if (LOG.isDebugEnabled) {
            LOG.debug("Constructing RejectionMessage for requested artifact: {}", rejectionReason)
        }
        val rejectionMessageBuilder = RejectionMessageBuilder()
        Utils.initMessageBuilder(rejectionMessageBuilder)
        rejectionMessageBuilder
                ._correlationMessage_(artifactRequestMessage.correlationMessage)
                ._rejectionReason_(rejectionReason)
        rejectionMessageBuilder.build().let {
            if (LOG.isDebugEnabled) {
                LOG.debug("Serialisation header: {}", SERIALIZER.serialize(it))
            }
            exchange.message.setHeader(IDSCP2_HEADER, it)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactRequestProcessor::class.java)
    }

}