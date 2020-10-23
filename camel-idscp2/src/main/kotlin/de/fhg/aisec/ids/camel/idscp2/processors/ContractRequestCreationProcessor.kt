package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent
import de.fhg.aisec.ids.camel.idscp2.processors.Utils.SERIALIZER
import de.fraunhofer.iais.eis.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

class ContractRequestCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        val requestMessageBuilder = ContractRequestBuilder()
        Idscp2OsgiComponent.infoModelManager.initMessageBuilder(requestMessageBuilder)
        val artifactUri = exchange.getProperty(ARTIFACT_URI_PROPERTY).let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        val requestMessageSerialization = SERIALIZER.serialize(requestMessageBuilder.build())
        if (LOG.isDebugEnabled) {
            LOG.debug("ContractRequestMessage serialization: {}", requestMessageSerialization)
        }
        exchange.message.setHeader(IDSCP2_HEADER, requestMessageSerialization)
        val contractOffer = ContractOfferBuilder()
                ._permission_(ArrayList<Permission>().also { pl ->
                    pl += PermissionBuilder()
                            ._target_(artifactUri)
                            ._constraint_(ArrayList<Constraint>().also { cl ->
                                cl += ConstraintBuilder()
                                        ._leftOperand_(LeftOperand.SYSTEM)
                                        ._operator_(BinaryOperator.SAME_AS)
                                        ._rightOperandReference_(URI.create(""))
                                        .build()
                            })
                            .build()
                })
                .build()
        exchange.message.body = SERIALIZER.serialize(contractOffer)
    }

    companion object {
        const val ARTIFACT_URI_PROPERTY = "artifactUri"

        private val LOG = LoggerFactory.getLogger(ContractRequestCreationProcessor::class.java)
    }

}