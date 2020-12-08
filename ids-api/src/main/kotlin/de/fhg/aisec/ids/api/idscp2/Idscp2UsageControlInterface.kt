package de.fhg.aisec.ids.api.idscp2

import de.fraunhofer.iais.eis.ContractAgreement
import org.apache.camel.Exchange
import java.net.URI

/**
 * This interface has to be parameterized with the type of the connection objects handled,
 * e.g. AppLayerConnection objects.
 * This avoids the necessity of library dependencies to the module defining this class.
 */
interface Idscp2UsageControlInterface {
    /**
     * Provides the (Idscp2)Connection object that is linked with the Apache Camel Consumer
     * which created the particular Exchange object.
     */
    fun getExchangeContract(exchange: Exchange): ContractAgreement?

    fun isProtected(exchange: Exchange): Boolean

    fun protectBody(exchange: Exchange, contractUri: URI)

    fun unprotectBody(exchange: Exchange)
}