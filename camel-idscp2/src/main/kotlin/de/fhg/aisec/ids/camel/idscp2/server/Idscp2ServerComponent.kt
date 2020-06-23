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

import de.fhg.aisec.ids.camel.idscp2.RefCountingHashMap
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import org.apache.camel.Endpoint
import org.apache.camel.spi.annotations.Component
import org.apache.camel.support.DefaultComponent

@Component("idscp2server")
class Idscp2ServerComponent : DefaultComponent() {
    private val servers = RefCountingHashMap<Idscp2Settings, CamelIdscp2Server> {
        it.terminate()
    }

    init {
        RatProverDriverRegistry.getInstance().registerDriver(
                "Dummy", RatProverDummy::class.java, null)
        RatVerifierDriverRegistry.getInstance().registerDriver(
                "Dummy", RatVerifierDummy::class.java, null)
        RatProverDriverRegistry.getInstance().registerDriver(
                "TPM2d", TPM2dProver::class.java,
                TPM2dProverConfig.Builder().build()
        )
        RatVerifierDriverRegistry.getInstance().registerDriver(
                "TPM2d", TPM2dVerifier::class.java,
                TPM2dVerifierConfig.Builder().build()
        )
    }

    override fun createEndpoint(uri: String, remaining: String, parameters: Map<String, Any>): Endpoint {
        val endpoint: Endpoint = Idscp2ServerEndpoint(uri, remaining, this)
        setProperties(endpoint, parameters)
        return endpoint
    }

    @Synchronized
    fun getServer(serverSettings: Idscp2Settings) = servers.computeIfAbsent(serverSettings) { CamelIdscp2Server(it) }

    @Synchronized
    fun freeServer(serverSettings: Idscp2Settings) = servers.release(serverSettings)

    @Synchronized
    override fun doStop() {
        servers.freeAll()
        super.doStop()
    }
}