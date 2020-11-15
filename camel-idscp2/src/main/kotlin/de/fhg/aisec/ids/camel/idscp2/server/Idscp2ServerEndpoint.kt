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
import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2ConnectionListener
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.configuration.Idscp2Configuration
import org.apache.camel.Processor
import org.apache.camel.Producer
import org.apache.camel.spi.UriEndpoint
import org.apache.camel.spi.UriParam
import org.apache.camel.support.DefaultEndpoint
import org.apache.camel.support.jsse.SSLContextParameters
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

@UriEndpoint(
        scheme = "idscp2server",
        title = "IDSCP2 Server Socket",
        syntax = "idscp2server://host:port",
        label = "ids"
)
class Idscp2ServerEndpoint(uri: String?, private val remaining: String, component: Idscp2ServerComponent?) :
        DefaultEndpoint(uri, component), Idscp2EndpointListener<AppLayerConnection> {
    private lateinit var serverConfiguration: Idscp2Configuration
    private var server: CamelIdscp2Server? = null
    private val consumers: MutableSet<Idscp2ServerConsumer> = HashSet()

    @UriParam(
            label = "security",
            description = "The SSL context for the IDSCP2 endpoint"
    )
    var sslContextParameters: SSLContextParameters? = null
    @UriParam(
            label = "security",
            description = "The alias of the DAPS key in the keystore provided by sslContextParameters",
            defaultValue = "1"
    )
    var dapsKeyAlias: String = "1"
    @UriParam(
            label = "security",
            description = "The validity time of remote attestation and DAT in milliseconds",
            defaultValue = "600000"
    )
    var dapsRatTimeoutDelay: Long = AttestationConfig.DEFAULT_RAT_TIMEOUT_DELAY.toLong()

    @Synchronized
    fun addConsumer(consumer: Idscp2ServerConsumer) {
        consumers.add(consumer)
        server?.let { server -> server.allConnections.forEach { it.addGenericMessageListener(consumer) } }
    }

    @Synchronized
    fun removeConsumer(consumer: Idscp2ServerConsumer) {
        server?.let { server -> server.allConnections.forEach { it.removeGenericMessageListener(consumer) } }
        consumers.remove(consumer)
    }

    @Synchronized
    fun sendMessage(type: String?, body: ByteArray?) {
        server?.let { server -> server.allConnections.forEach { it.sendGenericMessage(type, body) } }
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
    override fun onConnection(connection: AppLayerConnection) {
        LOG.debug("New IDSCP2 connection on $endpointUri, register consumer listeners")
        consumers.forEach { connection.addGenericMessageListener(it) }
        // Handle connection errors and closing
        connection.addConnectionListener(object : Idscp2ConnectionListener {
            override fun onError(t: Throwable) {
                LOG.error("Error in Idscp2ServerEndpoint-managed connection", t)
            }
            override fun onClose() {
                consumers.forEach { connection.removeGenericMessageListener(it) }
            }
        })
    }

    override fun onError(t: Throwable) {
        LOG.error("Error in IDSCP2 server endpoint $endpointUri", t)
    }

    @Synchronized
    public override fun doStart() {
        LOG.debug("Starting IDSCP2 server endpoint $endpointUri")
        val remainingMatcher = URI_REGEX.matcher(remaining)
        require(remainingMatcher.matches()) { "$remaining is not a valid URI remainder, must be \"host:port\"." }
        val matchResult = remainingMatcher.toMatchResult()
        val host = matchResult.group(1)
        val port = matchResult.group(2)?.toInt() ?: Idscp2Configuration.DEFAULT_SERVER_PORT
        val localAttestationConfig = AttestationConfig.Builder()
                .setSupportedRatSuite(arrayOf(RatProverDummy.RAT_PROVER_DUMMY_ID, TPM2dProver.TPM_RAT_PROVER_ID))
                .setExpectedRatSuite(arrayOf(RatVerifierDummy.RAT_VERIFIER_DUMMY_ID, TPM2dVerifier.TPM_RAT_VERIFIER_ID))
                .setRatTimeoutDelay(dapsRatTimeoutDelay)
                .build()
        val serverConfigurationBuilder = Idscp2Configuration.Builder()
                .setHost(host)
                .setServerPort(port)
                .setAttestationConfig(localAttestationConfig)
                .setDapsKeyAlias(dapsKeyAlias)
        sslContextParameters?.let {
            serverConfigurationBuilder
                    .setKeyPassword(it.keyManagers?.keyPassword?.toCharArray()
                            ?: "password".toCharArray())
                    .setKeyStorePath(Paths.get(it.keyManagers?.keyStore?.resource ?: "DUMMY-FILENAME.p12"))
                    .setKeyStoreKeyType(it.keyManagers?.keyStore?.type ?: "RSA")
                    .setKeyStorePassword(it.keyManagers?.keyStore?.password?.toCharArray()
                            ?: "password".toCharArray())
                    .setTrustStorePath(Paths.get(it.trustManagers?.keyStore?.resource ?: "DUMMY-FILENAME.p12"))
                    .setTrustStorePassword(it.trustManagers?.keyStore?.password?.toCharArray()
                            ?: "password".toCharArray())
                    .setCertificateAlias(it.certAlias ?: "1.0.1")
        }
        serverConfiguration = serverConfigurationBuilder.build()
        (component as Idscp2ServerComponent).getServer(serverConfiguration).let {
            server = it
            // Add this endpoint to this server's Idscp2EndpointListener set
            it.listeners += this
        }
    }

    @Synchronized
    public override fun doStop() {
        LOG.debug("Stopping IDSCP2 server endpoint $endpointUri")
        // Remove this endpoint from the server's Idscp2EndpointListener set
        server?.let { it.listeners -= this }
        (component as Idscp2ServerComponent).freeServer(serverConfiguration)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ServerEndpoint::class.java)
        private val URI_REGEX = Pattern.compile("(.*?)(?::(\\d+))?/?$")
    }
}