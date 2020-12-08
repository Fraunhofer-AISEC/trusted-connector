package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.ProviderDB
import de.fhg.aisec.ids.camel.idscp2.UsageControlMaps
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.ContractAgreement
import de.fraunhofer.iais.eis.ContractAgreementMessage
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class ContractAgreementReceiverProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractAgreementMessage = exchange.message.getHeader(
                IDSCP2_HEADER, ContractAgreementMessage::class.java)

        val contractAgreement = SERIALIZER.deserialize(
                exchange.message.getBody(String::class.java),
                ContractAgreement::class.java)

        UsageControlMaps.addContractAgreement(contractAgreement)
        if (LOG.isDebugEnabled) {
            LOG.debug("Provider is saving contract ${contractAgreement.id}")
        }

        ProviderDB.contractAgreements[contractAgreement.id] = contractAgreement
        for (permission in contractAgreement.permission) {
            ProviderDB.artifactUrisMapped2ContractAgreements[permission.target] = contractAgreement.id
        }

        if (LOG.isDebugEnabled) {
            LOG.debug("Saved Agreement {}", contractAgreement.id)
        }
        if (LOG.isDebugEnabled) {
            LOG.debug("Received ContractAgreementMessage {}", SERIALIZER.serialize(contractAgreementMessage))
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractAgreementReceiverProcessor::class.java)
    }

}