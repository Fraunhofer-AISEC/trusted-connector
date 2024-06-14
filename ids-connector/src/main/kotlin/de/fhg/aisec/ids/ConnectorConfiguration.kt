/*-
 * ========================LICENSE_START=================================
 * ids-connector
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
package de.fhg.aisec.ids

import de.fhg.aisec.ids.api.LazyProducer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.camel.idscp2.ListenerManager
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.processors.UsageControlMaps
import de.fhg.aisec.ids.cmc.CmcConfig
import de.fhg.aisec.ids.cmc.prover.CmcProver
import de.fhg.aisec.ids.cmc.prover.CmcProverConfig
import de.fhg.aisec.ids.cmc.verifier.CmcVerifier
import de.fhg.aisec.ids.cmc.verifier.CmcVerifierConfig
import de.fhg.aisec.ids.idscp2.api.raregistry.RaProverDriverRegistry
import de.fhg.aisec.ids.idscp2.api.raregistry.RaVerifierDriverRegistry
import de.fhg.aisec.ids.rm.RouteManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

@Configuration
class ConnectorConfiguration {
    @Autowired(required = false)
    private var cml: ContainerManager? = null

    @Autowired
    private lateinit var settings: Settings

    @Autowired
    private lateinit var im: InfoModel

    @Autowired
    private lateinit var rm: RouteManagerService

    @Value("\${connector.daps-url}")
    private lateinit var dapsUrl: String

    @Value("\${connector.connector-url}")
    private lateinit var connectorUrl: String

    @Value("\${connector.sender-agent}")
    private lateinit var senderAgent: String

    @Value("\${idscp2.cmc-endpoint:127.0.0.1}")
    private lateinit var cmcEndpoint: String

    @Bean
    fun configureIdscp2(): CommandLineRunner =
        CommandLineRunner {
            Utils.issuerProducer =
                LazyProducer {
                    if (connectorUrl.isNotBlank()) {
                        URI.create(connectorUrl)
                    } else {
                        // Kept for backwards compatibility
                        settings.connectorProfile.connectorUrl
                            ?: URI.create("https://connector.ids")
                    }
                }
            Utils.senderAgentProducer =
                LazyProducer {
                    if (senderAgent.isNotBlank()) {
                        URI.create(senderAgent)
                    } else {
                        // Kept for backwards compatibility
                        settings.connectorProfile.maintainerUrl
                            ?: URI.create("https://sender-agent.ids")
                    }
                }
            Utils.dapsUrlProducer =
                LazyProducer {
                    dapsUrl.ifBlank {
                        settings.connectorConfig.dapsUrl
                    }
                }
            TrustedConnector.LOG.info("Information model {} loaded", BuildConfig.INFOMODEL_VERSION)
            Utils.infomodelVersion = BuildConfig.INFOMODEL_VERSION

            ListenerManager.addExchangeListener { connection, exchange ->
                UsageControlMaps.setExchangePeerIdentity(exchange, connection.peerDat.identity)
            }
            ListenerManager.addTransferContractListener { connection, transferContract ->
                UsageControlMaps.setPeerContract(connection.peerDat.identity, transferContract)
            }

            idscp2CmcRatConfig()
        }

    /**
     * Method for configuration of IDSCP2 CMC attestation driver.
     */
    private fun idscp2CmcRatConfig() {
        // RAT prover configuration
        val cmcHostAndPort = cmcEndpoint.split(":").toTypedArray()
        var cmcPort: Int = CmcConfig.DEFAULT_CMC_PORT
        if (cmcHostAndPort.size > 1) {
            cmcPort = cmcHostAndPort[1].toInt()
        }
        val proverConfig: CmcProverConfig =
            CmcProverConfig
                .Builder()
                .setCmcHost(cmcHostAndPort[0])
                .setCmcPort(cmcPort)
                .build()
        RaProverDriverRegistry.registerDriver(
            CmcProver.ID,
            { fsmListener -> CmcProver(fsmListener) },
            proverConfig
        )

        // RAT verifier configuration
        val verifierConfig: CmcVerifierConfig =
            CmcVerifierConfig
                .Builder()
                .setCmcHost(cmcHostAndPort[0])
                .setCmcPort(cmcPort)
                .build()
        RaVerifierDriverRegistry.registerDriver(
            CmcVerifier.ID,
            { fsmListener -> CmcVerifier(fsmListener) },
            verifierConfig
        )
    }

    @Bean
    fun listBeans(ctx: ApplicationContext): CommandLineRunner =
        CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                ctx.beanDefinitionNames.sorted().forEach {
                    TrustedConnector.LOG.debug("Loaded bean: {}", it)
                }
            }
        }

    @Bean
    fun listContainers(ctx: ApplicationContext): CommandLineRunner =
        CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                cml?.list(false)?.forEach {
                    TrustedConnector.LOG.debug("Container: {}", it.names)
                }
            }
        }

    @Bean
    fun showConnectorProfile(ctx: ApplicationContext): CommandLineRunner =
        CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                im.connector?.let {
                    TrustedConnector.LOG.debug("Connector profile:\n{}", im.connectorAsJsonLd)
                } ?: TrustedConnector.LOG.debug("No connector profile stored yet.")
            }
        }

    @Bean
    fun showCamelInfo(ctx: ApplicationContext): CommandLineRunner =
        CommandLineRunner {
            val routes = rm.routes

            for (route in routes) {
                TrustedConnector.LOG.debug("Route: {}", route.shortName)
            }

            val components = rm.listComponents()

            for (component in components) {
                TrustedConnector.LOG.debug("Component: {}", component.bundle)
            }
        }
}
