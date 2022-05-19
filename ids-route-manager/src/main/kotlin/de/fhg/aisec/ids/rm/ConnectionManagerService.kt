/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
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
package de.fhg.aisec.ids.rm

import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint
import org.apache.camel.CamelContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ConnectionManagerService : ConnectionManager {
    @Autowired private lateinit var ctx: ApplicationContext

    private val camelContexts: List<CamelContext>
        get() {
            return try {
                val appContexts = ctx.getBeansOfType(CamelContext::class.java).values.toList()
                val watcherContexts = XmlDeployWatcher.getBeansOfType(CamelContext::class.java)
                return listOf(appContexts, watcherContexts).flatten().sortedBy { it.name }
            } catch (e: Exception) {
                RouteManagerService.LOG.warn("Cannot retrieve the list of Camel contexts.", e)
                emptyList()
            }
        }

    override fun listIncomingConnections(): List<IDSCPIncomingConnection> {
        var list: List<IDSCPIncomingConnection> = emptyList()
        var tmp = IDSCPIncomingConnection()
        for (cCtx in camelContexts) {
            for (ep in cCtx.endpoints) {
                when (ep) {
                    //  is Idscp2ServerEndpoint -> {ep.}
                    else -> {}
                }
                //  var ep2 = Idscp2ServerEndpoint()
                //  ep2 = ep as Idscp2ServerEndpoint
            }
        }

        list += tmp
        return list
    }

    override fun listOutgoingConnections(): List<IDSCPOutgoingConnection> {
        var list: List<IDSCPOutgoingConnection> = emptyList()

        for (cCtx in camelContexts) {
            var tmp = IDSCPOutgoingConnection()
            for ((key, value) in cCtx.endpointRegistry) {
                // cast

                list += tmp
            }

            //  cCtx.endpoints.
        }
        return list
    }

    override fun listAvailableEndpoints(): List<IDSCPServerEndpoint> {
        var list: List<IDSCPServerEndpoint> = emptyList()

        for (cCtx in camelContexts) {
            var tmp = IDSCPServerEndpoint()
            for ((key, value) in cCtx.endpointRegistry) {
                tmp.endpointIdentifier = value.endpointBaseUri
                tmp.defaultProtocol = value.endpointBaseUri.substringBefore(':')
                tmp.port = value.endpointBaseUri.substringAfter("://").substringAfter(':')
                tmp.host = value.endpointBaseUri.substringAfter("://").substringBefore(':')
                list += tmp
            }

            //  cCtx.endpoints.
        }

        /*
        //Test
        val tmp = IDSCPServerEndpoint()
        tmp.endpointIdentifier = "test"
        tmp.defaultProtocol = "test"
        tmp.port = "test"
        tmp.host = "test"
        // list.toMutableList().add(tmp)
        list += tmp
        */
        // Test
        println(message = "#################################")
        println(message = list.size)
        println(message = "#################################")

        return list
    }
}
