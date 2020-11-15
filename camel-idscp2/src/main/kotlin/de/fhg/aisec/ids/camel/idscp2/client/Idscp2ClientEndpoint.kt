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
package de.fhg.aisec.ids.camel.idscp2.client

import de.fhg.aisec.ids.camel.idscp2.Idscp2OsgiComponent
import de.fhg.aisec.ids.camel.idscp2.RefCountingHashMap
import de.fhg.aisec.ids.idscp2.app_layer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps.DefaultDapsDriverConfig
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatProverDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy.RatVerifierDummy
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dProver
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d.TPM2dVerifier
import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.NativeTLSDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.SecureChannelDriver
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
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

@UriEndpoint(
        scheme = "idscp2client",
        title = "IDSCP2 Client Socket",
        syntax = "idscp2client://host:port",
        label = "ids"
)
class Idscp2ClientEndpoint(uri: String?, private val remaining: String, component: Idscp2ClientComponent?) :
        DefaultEndpoint(uri, component) {
    private lateinit var secureChannelDriver: SecureChannelDriver<AppLayerConnection>
    private lateinit var dapsDriver: DapsDriver
    private lateinit var clientConfiguration: Idscp2Configuration

    @UriParam(
            label = "security",
            description = "The SSL context for the IDSCP2 endpoint"
    )
    var sslContextParameters: SSLContextParameters? = null
    @UriParam(
            label = "security",
            description = "The alias of the DAPS key in the keystore provided by sslContextParameters"
    )
    var dapsKeyAlias: String? = null
    @UriParam(
            label = "security",
            description = "The validity time of remote attestation and DAT in milliseconds",
            defaultValue = "600000"
    )
    var dapsRatTimeoutDelay: Long? = null
    @UriParam(
            label = "client",
            description = "Used to make N endpoints share the same connection, " +
                    "e.g. for using a consumer to receive responses to the requests of another producer"
    )
    var connectionShareId: String? = null

    private fun makeConnectionInternal(): CompletableFuture<AppLayerConnection> {
        return secureChannelDriver.connect(::AppLayerConnection, clientConfiguration, dapsDriver)
    }

    fun makeConnection(): CompletableFuture<AppLayerConnection> {
        connectionShareId?.let {
            return sharedConnections.computeIfAbsent(it) {
                makeConnectionInternal()
            }
        } ?: return makeConnectionInternal()
    }

    fun releaseConnection(connectionFuture: CompletableFuture<AppLayerConnection>) {
        connectionShareId?.let { sharedConnections.release(it) } ?: releaseConnectionInternal(connectionFuture)
    }

    override fun createProducer(): Producer {
        return Idscp2ClientProducer(this)
    }

    override fun createConsumer(processor: Processor): org.apache.camel.Consumer {
        return Idscp2ClientConsumer(this, processor)
    }

    public override fun doStart() {
        LOG.debug("Starting IDSCP2 client endpoint $endpointUri")
        val remainingMatcher = URI_REGEX.matcher(remaining)
        require(remainingMatcher.matches()) { "$remaining is not a valid URI remainder, must be \"host:port\"." }
        val matchResult = remainingMatcher.toMatchResult()
        val host = matchResult.group(1)
        val port = matchResult.group(2)?.toInt() ?: Idscp2Configuration.DEFAULT_SERVER_PORT
        val localAttestationConfig = AttestationConfig.Builder()
                .setSupportedRatSuite(arrayOf(RatProverDummy.RAT_PROVER_DUMMY_ID, TPM2dProver.TPM_RAT_PROVER_ID))
                .setExpectedRatSuite(arrayOf(RatVerifierDummy.RAT_VERIFIER_DUMMY_ID, TPM2dVerifier.TPM_RAT_VERIFIER_ID))
                .setRatTimeoutDelay(dapsRatTimeoutDelay ?: AttestationConfig.DEFAULT_RAT_TIMEOUT_DELAY.toLong())
                .build()
        val clientConfigurationBuilder = Idscp2Configuration.Builder()
                .setHost(host)
                .setServerPort(port)
                .setAttestationConfig(localAttestationConfig)
                .setDapsKeyAlias(dapsKeyAlias ?: "1")
        sslContextParameters?.let {
            clientConfigurationBuilder
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
        secureChannelDriver = NativeTLSDriver()
        clientConfiguration = clientConfigurationBuilder.build()
        dapsDriver = DefaultDapsDriver(DefaultDapsDriverConfig.Builder()
                .setDapsUrl(Idscp2OsgiComponent.settings.connectorConfig.dapsUrl)
                .setKeyAlias(clientConfiguration.dapsKeyAlias)
                .setKeyPassword(clientConfiguration.keyPassword)
                .setKeyStorePath(clientConfiguration.keyStorePath)
                .setTrustStorePath(clientConfiguration.trustStorePath)
                .setKeyStorePassword(clientConfiguration.keyStorePassword)
                .setTrustStorePassword(clientConfiguration.trustStorePassword)
                .build())
    }

    public override fun doStop() {
        LOG.debug("Stopping IDSCP2 client endpoint $endpointUri")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Idscp2ClientEndpoint::class.java)
        private val URI_REGEX = Pattern.compile("(.*?)(?::(\\d+))?/?$")
        private val sharedConnections = RefCountingHashMap<String, CompletableFuture<AppLayerConnection>> {
            releaseConnectionInternal(it)
        }
        private fun releaseConnectionInternal(connectionFuture: CompletableFuture<AppLayerConnection>) {
            if (connectionFuture.isDone) {
                connectionFuture.get().close()
            } else if (!connectionFuture.isCompletedExceptionally) {
                connectionFuture.cancel(true)
            }
        }
    }
}