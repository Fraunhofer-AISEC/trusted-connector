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
import de.fhg.aisec.ids.camel.idscp2.ListenerManager
import de.fhg.aisec.ids.camel.idscp2.client.Idscp2ClientEndpoint
import de.fhg.aisec.ids.camel.idscp2.listeners.ConnectionListener
import de.fhg.aisec.ids.camel.idscp2.server.Idscp2ServerEndpoint
import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.idscp_core.api.idscp_connection.Idscp2ConnectionListener
import org.apache.camel.CamelContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

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
        }
        return list
    }

    // TODO: Register Listener, get connection information and return results in listOutgoing/IncomingConnections()

    // Register a connection listener with idscp2-camel.
    // The connection listener is notified each time a new connection is created.
    // We use this in order to make a list of connections available to the web console

    val connectionListener = object : ConnectionListener {
        override fun onClientConnection(connection: AppLayerConnection, endpoint: Idscp2ClientEndpoint) {
            // first register a idscp2connectionListener to keep track of connection cleanup
            connection.addConnectionListener(object : Idscp2ConnectionListener {
                override fun onError(t: Throwable) {
                    // TODO connection error handling
                }

                override fun onClose() {
                    // TODO connection was closed
                }
            })

            // TODO handle information from connection and endpoint
            val remotePeer = connection.remotePeer()
        }

        override fun onServerConnection(connection: AppLayerConnection, endpoint: Idscp2ServerEndpoint) {
            // TODO do the same for server connections
        }
    }

    @PostConstruct
    private fun registerConnectionListener() {
        ListenerManager.addConnectionListener(connectionListener)
    }

    @PreDestroy
    private fun deregisterConnectionListener() {
        // TODO: Also remove all idscp2 connection listeners
        ListenerManager.removeConnectionListener(connectionListener)
    }

    override fun listIncomingConnections(): List<IDSCPIncomingConnection> {
        var list: List<IDSCPIncomingConnection> = emptyList()
        var tmp = IDSCPIncomingConnection()
        for (cCtx in camelContexts) {
            for (ep in cCtx.endpoints) {
                when (ep) {
                    else -> {}
                }
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
                list += tmp
            }
        }
        return list
    }
}
