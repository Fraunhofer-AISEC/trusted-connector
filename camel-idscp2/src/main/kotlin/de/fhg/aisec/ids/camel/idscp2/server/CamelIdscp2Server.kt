/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.aisec.ids.camel.idscp2.server

import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent
import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener
import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2ServerFactory
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.server.Idscp2Server
import java.util.*

class CamelIdscp2Server(serverSettings: Idscp2Settings) : Idscp2EndpointListener<AppLayerConnection> {
    private val server: Idscp2Server<AppLayerConnection>
    val listeners: MutableSet<Idscp2EndpointListener<AppLayerConnection>> = Collections.synchronizedSet(HashSet())

    init {
        val dapsDriverConfig = DefaultDapsDriverConfig.Builder()
                .setDapsUrl(Idscp2OsgiComponent.getSettings().connectorConfig.dapsUrl)
                .setKeyAlias(serverSettings.dapsKeyAlias)
                .setKeyPassword(serverSettings.keyPassword)
                .setKeyStorePath(serverSettings.keyStorePath)
                .setTrustStorePath(serverSettings.trustStorePath)
                .setKeyStorePassword(serverSettings.keyStorePassword)
                .setTrustStorePassword(serverSettings.trustStorePassword)
                .build()
        val serverFactory = Idscp2ServerFactory(
                ::AppLayerConnection,
                this,
                serverSettings,
                DefaultDapsDriver(dapsDriverConfig),
                NativeTLSDriver()
        )
        server = serverFactory.listen(serverSettings)
    }

    override fun onConnection(connection: AppLayerConnection) {
        listeners.forEach { it.onConnection(connection) }
    }

    override fun onError(t: Throwable) {
        listeners.forEach { it.onError(t) }
    }

    val allConnections: Collection<AppLayerConnection> = server.allConnections

    fun terminate() {
        server.terminate()
    }
}