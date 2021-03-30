/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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