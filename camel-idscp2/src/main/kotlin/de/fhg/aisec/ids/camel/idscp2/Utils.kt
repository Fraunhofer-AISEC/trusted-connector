package de.fhg.aisec.ids.camel.idscp2

import de.fraunhofer.iais.eis.DynamicAttributeToken
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder
import de.fraunhofer.iais.eis.TokenFormat
import de.fraunhofer.iais.eis.ids.jsonld.Serializer
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object Utils {
    val SERIALIZER: Serializer by lazy { Serializer() }
    private val LOG = LoggerFactory.getLogger(Utils::class.java)

    lateinit var dynamicAttributeTokenProducer: () -> String
    lateinit var maintainerUrlProducer: () -> URI
    lateinit var connectorUrlProducer: () -> URI
    lateinit var infomodelVersion: String
    var dapsUrlProducer: () -> String = { Constants.DEFAULT_DAPS_URL }

    fun createGregorianCalendarTimestamp(timeInput: Long): XMLGregorianCalendar? {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                GregorianCalendar().apply { timeInMillis = timeInput })
    }

    fun <T: Any> initMessageBuilder(builder: T): T {
        try {
            builder::class.java.apply {
                getMethod("_securityToken_", DynamicAttributeToken::class.java)
                        .invoke(builder, DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)
                                ._tokenValue_(dynamicAttributeTokenProducer()).build())
                getMethod("_senderAgent_", URI::class.java).invoke(builder, maintainerUrlProducer())
                getMethod("_issuerConnector_", URI::class.java).invoke(builder, connectorUrlProducer())
                getMethod("_issued_", XMLGregorianCalendar::class.java)
                        .invoke(builder, createGregorianCalendarTimestamp(System.currentTimeMillis()))
                getMethod("_modelVersion_", String::class.java).invoke(builder, infomodelVersion)
            }
        } catch (upa: UninitializedPropertyAccessException) {
            LOG.error("At least one property of de.fhg.aisec.ids.camel.idscp2.Utils has not been " +
                    "properly initialized. This is a mandatory requirement for initialization " +
                    "of IDSCP Messages within the IDSCP2 Camel Adapter!")
        }
        return builder
    }
}

