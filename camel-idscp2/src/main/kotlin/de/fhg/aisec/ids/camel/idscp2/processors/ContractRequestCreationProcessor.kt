package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.ARTIFACT_URI_PROPERTY
import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ContractRequestBuilder
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder
import de.fraunhofer.iais.eis.Permission
import de.fraunhofer.iais.eis.PermissionBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

class ContractRequestCreationProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val requestMessageBuilder = ContractRequestMessageBuilder()
        Utils.initMessageBuilder(requestMessageBuilder)
        requestMessageBuilder.build().let {
            if (LOG.isDebugEnabled) LOG.debug("Serialization header: {}",SERIALIZER.serialize(it))
            exchange.message.setHeader(IDSCP2_HEADER, it)
        }

        val artifactUri = exchange.getProperty(ARTIFACT_URI_PROPERTY)?.let {
            if (it is URI) {
                it
            } else {
                URI.create(it.toString())
            }
        }
        // setting creation/start date of contract to now
        val contractDate = Utils.createGregorianCalendarTimestamp(System.currentTimeMillis())
        val contractRequest = ContractRequestBuilder()
                ._contractDate_(contractDate)
                ._contractStart_(contractDate)
                // Contract end one year in the future
                ._contractEnd_(contractDate?.apply { year += 1 })
                // Request permission for (unrestricted?) usage of an artifact, identified by URI
                ._permission_(ArrayList<Permission>().also { pl ->
                    pl += PermissionBuilder()
                            ._target_(artifactUri)
                            .build()
                })
                .build()
        SERIALIZER.serialize(contractRequest).let {
            if (LOG.isDebugEnabled) LOG.debug("Serialization body: {}", it)
            exchange.message.body = it
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractRequestCreationProcessor::class.java)
    }

}