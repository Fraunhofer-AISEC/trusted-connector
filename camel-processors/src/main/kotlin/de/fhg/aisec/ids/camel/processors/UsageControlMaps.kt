/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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
package de.fhg.aisec.ids.camel.processors

import com.google.common.collect.MapMaker
import de.fhg.aisec.ids.idscp2.applayer.AppLayerConnection
import de.fraunhofer.iais.eis.ContractAgreement
import org.apache.camel.Exchange
import org.slf4j.LoggerFactory
import java.net.URI

object UsageControlMaps {
    private val LOG = LoggerFactory.getLogger(Utils::class.java)

    private val contractMap: MutableMap<URI, ContractAgreement> =
        MapMaker().makeMap()
    private val exchangePeerIdentityMap: MutableMap<Exchange, String> =
        MapMaker().weakKeys().makeMap()
    private val peerContracts: MutableMap<String, URI> =
        MapMaker().weakKeys().makeMap()

    fun getExchangePeerIdentity(exchange: Exchange): String? =
        exchangePeerIdentityMap[exchange]

    fun getExchangeContract(exchange: Exchange): ContractAgreement? {
        return exchangePeerIdentityMap[exchange]?.let { connection ->
            peerContracts[connection]?.let { uri ->
                contractMap[uri] ?: throw RuntimeException("Contract $uri is not available!")
            }
        }
    }

    fun addContractAgreement(contractAgreement: ContractAgreement) {
        contractMap[contractAgreement.id] = contractAgreement
    }

    fun setConnectionContract(connection: AppLayerConnection, contractUri: URI?) {
        if (contractUri != null) {
            peerContracts[connection.peerDat.identity] = contractUri
            if (LOG.isDebugEnabled) {
                LOG.debug("UC: Assigned contract $contractUri to connection $connection")
            }
        } else {
            peerContracts -= connection.peerDat.identity
            if (LOG.isDebugEnabled) {
                LOG.debug("UC: Assigned no contract to connection $connection")
            }
        }
    }

    fun setExchangeConnection(exchange: Exchange, connection: AppLayerConnection) {
        exchangePeerIdentityMap[exchange] = connection.peerDat.identity
        if (LOG.isDebugEnabled) {
            LOG.debug("UC: Assigned exchange $exchange to connection $connection")
        }
    }
}
