package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants
import de.fhg.aisec.ids.camel.idscp2.Constants.CONTAINER_URI_PROPERTY
import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * This Processor handles a ContractRequestMessage and creates a ContractResponseMessage.
 */
class ContractOfferCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractOfferMessageBuilder = ContractOfferMessageBuilder()
        Utils.initMessageBuilder(contractOfferMessageBuilder)
        contractOfferMessageBuilder.build().let {
            if (LOG.isDebugEnabled) {
                LOG.debug("Serialization header: {}",SERIALIZER.serialize(it))
            }
            exchange.message.setHeader(IDSCP2_HEADER, it)
        }

        // create ContractOffer, allowing use of received data in the given container only
        val artifactUri = exchange.getProperty(Constants.ARTIFACT_URI_PROPERTY)?.let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        val containerUri = exchange.getProperty(CONTAINER_URI_PROPERTY).let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        val contractOffer = ContractOfferBuilder()
                ._permission_(ArrayList<Permission>().also { pl ->
                    pl += PermissionBuilder()
                            ._target_(artifactUri)
                            ._constraint_(ArrayList<Constraint>().also { cl ->
                                cl += ConstraintBuilder()
                                        ._leftOperand_(LeftOperand.SYSTEM)
                                        ._operator_(BinaryOperator.SAME_AS)
                                        ._rightOperandReference_(containerUri)
                                        .build()
                            })
                            .build()
                })
                .build()

        SERIALIZER.serialize(contractOffer).let {
            if (LOG.isDebugEnabled) {
                LOG.debug("ContractOffer ID: {}", contractOffer.id)
                LOG.debug("Serialisation body: {}", it)
            }
            exchange.message.body = it
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferCreationProcessor::class.java)
    }

}