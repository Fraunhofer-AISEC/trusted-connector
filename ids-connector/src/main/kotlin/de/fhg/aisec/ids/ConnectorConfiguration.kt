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

    @Bean
    fun configureIdscp2(): CommandLineRunner {
        return CommandLineRunner {
            Utils.issuerProducer = LazyProducer {
                if (connectorUrl.isNotBlank()) {
                    URI.create(connectorUrl)
                } else {
                    // Kept for backwards compatibility
                    settings.connectorProfile.connectorUrl
                        ?: URI.create("https://connector.ids")
                }
            }
            Utils.senderAgentProducer = LazyProducer {
                if (senderAgent.isNotBlank()) {
                    URI.create(senderAgent)
                } else {
                    // Kept for backwards compatibility
                    settings.connectorProfile.maintainerUrl
                        ?: URI.create("https://sender-agent.ids")
                }
            }
            Utils.dapsUrlProducer = LazyProducer {
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
        }
    }

    @Bean
    fun listBeans(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                ctx.beanDefinitionNames.sorted().forEach {
                    TrustedConnector.LOG.debug("Loaded bean: {}", it)
                }
            }
        }
    }

    @Bean
    fun listContainers(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                cml?.list(false)?.forEach {
                    TrustedConnector.LOG.debug("Container: {}", it.names)
                }
            }
        }
    }

    @Bean
    fun showConnectorProfile(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            if (TrustedConnector.LOG.isDebugEnabled) {
                im.connector?.let {
                    TrustedConnector.LOG.debug("Connector profile:\n{}", im.connectorAsJsonLd)
                } ?: TrustedConnector.LOG.debug("No connector profile stored yet.")
            }
        }
    }

    @Bean
    fun showCamelInfo(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
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
}
