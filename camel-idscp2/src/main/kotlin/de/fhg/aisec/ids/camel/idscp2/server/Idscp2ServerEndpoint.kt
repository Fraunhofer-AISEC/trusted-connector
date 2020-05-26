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

import de.fhg.aisec.ids.idscp2.Idscp2EndpointListener
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifierConfig
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Settings
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import org.apache.camel.Processor
import org.apache.camel.Producer
import org.apache.camel.spi.UriEndpoint
import org.apache.camel.support.DefaultEndpoint
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

@UriEndpoint(
        scheme = "idscp2server",
        title = "IDSCP2 Server Socket",
        syntax = "idscp2server://host:port",
        label = "ids"
)
class Idscp2ServerEndpoint(uri: String?, remaining: String, component: Idscp2ServerComponent?) :
        DefaultEndpoint(uri, component), Idscp2EndpointListener {
    private val serverSettings: Idscp2Settings
    private lateinit var server: CamelIdscp2Server
    private val consumers: MutableSet<Idscp2ServerConsumer> = HashSet()

    init {
        val remainingMatcher = URI_REGEX.matcher(remaining)
        require(remainingMatcher.matches()) { "$remaining is not a valid URI remainder, must be \"host:port\"." }
        val matchResult = remainingMatcher.toMatchResult()
        val host = matchResult.group(1)
        val port = matchResult.group(2).toInt()
        serverSettings = Idscp2Settings.Builder()
                .setHost(host)
                .setServerPort(port)
                .setKeyStorePath("etc/idscp2/aisecconnector1-keystore.jks")
                .setTrustStorePath("etc/idscp2/client-truststore_new.jks")
                .setCertificateAlias("1.0.1")
                .setDapsKeyAlias("1")
                .setRatTimeoutDelay(300)
                .build()
    }

    @Synchronized
    fun addConsumer(consumer: Idscp2ServerConsumer) {
        consumers.add(consumer)
        server.allConnections.forEach { it.addGenericMessageListener(consumer) }
    }

    @Synchronized
    fun removeConsumer(consumer: Idscp2ServerConsumer) {
        consumers.remove(consumer)
        server.allConnections.forEach { it.removeGenericMessageListener(consumer) }
    }

    @Synchronized
    fun sendMessage(type: String, body: ByteArray) {
        server.allConnections.forEach { it.send(type, body) }
    }

    @Synchronized
    override fun createProducer(): Producer {
        return Idscp2ServerProducer(this)
    }

    @Synchronized
    override fun createConsumer(processor: Processor): org.apache.camel.Consumer {
        return Idscp2ServerConsumer(this, processor)
    }

    @Synchronized
    override fun onConnection(connection: Idscp2Connection) {
        LOG.debug("New IDSCP2 connection on $endpointUri, register consumer listeners")
        consumers.forEach { connection.addGenericMessageListener(it) }
    }

    override fun onError(error: String) {
        LOG.error("Error in IDSCP2 server endpoint $endpointUri:\n$error")
    }

    @Synchronized
    public override fun doStart() {
        LOG.debug("Starting IDSCP2 server endpoint $endpointUri")
        server = (component as Idscp2ServerComponent).getServer(serverSettings)
        // Add this endpoint to this server's Idscp2EndpointListener set
        server.listeners += this
    }

    @Synchronized
    public override fun doStop() {
        LOG.debug("Stopping IDSCP2 server endpoint $endpointUri")
        // Remove this endpoint from the server's Idscp2EndpointListener set
        server.listeners -= this
        (component as Idscp2ServerComponent).freeServer(serverSettings)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ServerEndpoint::class.java)
        private val URI_REGEX = Pattern.compile("(.*?):(\\d+)$")
    }
}