package de.fhg.aisec.ids.camel.idscp2.processors

import de.fhg.aisec.ids.camel.idscp2.Constants.IDSCP2_HEADER
import de.fhg.aisec.ids.camel.idscp2.UsageControlMaps
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.Utils.SERIALIZER
import de.fraunhofer.iais.eis.*
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * This Processor handles a ContractResponseMessage and creates a ContractAgreementMessage.
 */
class ContractOfferProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractOfferMessage = exchange.message.getHeader(
                IDSCP2_HEADER, ContractOfferMessage::class.java)

        val contractOfferReceived = SERIALIZER.deserialize(
                exchange.message.getBody(String::class.java),
                ContractOffer::class.java)

        // if contract is denied send ContractRejectionMsg else send ContractAgreementMsg
        val contractOfferIsAccepted = true
        if(!contractOfferIsAccepted){
            createContractRejectionMessage(exchange, contractOfferMessage.id)
        } else {
            ContractAgreementMessageBuilder().run {
                Utils.initMessageBuilder(this)
                _correlationMessage_(contractOfferMessage.id)
                build().let {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Serialization Header: {}", SERIALIZER.serialize(it))
                    }
                    exchange.message.setHeader(IDSCP2_HEADER, it)
                }
            }

            val contractAgreement = ContractAgreementBuilder()
                    ._consumer_(contractOfferReceived.consumer)
                    ._provider_(contractOfferReceived.provider)
                    ._contractAnnex_(contractOfferReceived.contractAnnex)
                    ._contractDate_(contractOfferReceived.contractDate)
                    ._contractDocument_(contractOfferReceived.contractDocument)
                    ._contractEnd_(contractOfferReceived.contractEnd)
                    ._contractStart_(contractOfferReceived.contractStart)
                    ._obligation_(contractOfferReceived.obligation)
                    ._prohibition_(contractOfferReceived.prohibition)
                    ._permission_(contractOfferReceived.permission)
                    .build()

            UsageControlMaps.addContractAgreement(contractAgreement)
            if (LOG.isDebugEnabled) {
                LOG.debug("Consumer saved contract ${contractAgreement.id}")
            }

            SERIALIZER.serialize(contractAgreement).let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("ContractAgreement ID: {}", contractAgreement.id)
                    LOG.debug("Serialization body: {}", it)
                }
                exchange.message.body = it
            }
        }
    }

    private fun createContractRejectionMessage(exchange: Exchange, correlationId: URI) {
        if (LOG.isDebugEnabled) {
            LOG.debug("Constructing ContractRejectionMessage")
        }
        ContractRejectionMessageBuilder().run {
            Utils.initMessageBuilder(this)
            _correlationMessage_(correlationId)
            build().let {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Serialization Header: {}", SERIALIZER.serialize(it))
                }
                exchange.message.setHeader(IDSCP2_HEADER, it)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferProcessor::class.java)
    }

}