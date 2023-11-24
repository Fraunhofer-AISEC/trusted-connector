/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole.api

import de.fhg.aisec.ids.api.Constants
import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.endpointconfig.EndpointConfigManager
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.api.settings.ConnectionSettings
import de.fhg.aisec.ids.api.settings.ConnectorConfig
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.ApiController
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import java.util.TreeMap
import java.util.regex.Pattern
import javax.ws.rs.core.MediaType

/**
 * REST API interface for configurations in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/config/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
</method> */
@ApiController
@RequestMapping("/config")
@Api(value = "Connector Configuration", authorizations = [Authorization(value = "oauth2")])
class ConfigApi {
    @Autowired
    private lateinit var settings: Settings

    @Autowired
    private lateinit var routeManager: RouteManager

    @Autowired(required = false)
    private var connectionManager: ConnectionManager? = null

    @Autowired(required = false)
    private var endpointConfigManager: EndpointConfigManager? = null

    @ApiOperation(value = "Retrieves the current configuration", response = ConnectorConfig::class)
    @GetMapping("/connectorConfig", produces = [MediaType.APPLICATION_JSON])
    fun get(): ConnectorConfig {
        return settings.connectorConfig
    }

    @PostMapping("/connectorConfig", consumes = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Sets the overall configuration of the connector")
    @ApiResponses(
        ApiResponse(
            code = 500,
            message = "_No valid preferences received_: If incorrect configuration parameter is provided"
        )
    )
    fun setConnectorConfig(
        @RequestBody config: ConnectorConfig
    ) {
        settings.connectorConfig = config
    }

    /**
     * Save connection configuration of a particular connection.
     *
     * @param connection The name of the connection
     * @param conSettings The connection configuration of the connection
     */
    @PostMapping("/connectionConfigs/{con}", consumes = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Save connection configuration of a particular connection")
    @ApiResponses(
        ApiResponse(
            code = 500,
            message = "_No valid connection settings received!_: If incorrect connection settings parameter is provided"
        )
    )
    fun setConnectionConfigurations(
        @PathVariable("con") connection: String,
        conSettings: ConnectionSettings
    ) {
        conSettings.let {
            // connection has format "<route_id> - host:port"
            // store only "host:port" in database to make connection available in other parts of the application
            // where rout_id is not available
            val m = CONNECTION_CONFIG_PATTERN.matcher(connection)
            if (!m.matches()) {
                // GENERAL_CONFIG has changed
                settings.setConnectionSettings(connection, it)
            } else {
                // specific endpoint config has changed
                settings.setConnectionSettings(m.group(1), it)

                // notify EndpointConfigurationListeners that some endpointConfig has changed
                endpointConfigManager?.notify(m.group(1))
            }
        } ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No valid connection settings received!")
    }

    /**
     * Sends configuration of a connection
     *
     * @param connection Connection identifier
     * @return The connection configuration of the requested connection
     */
    @GetMapping("/connectionConfigs/{con}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Sends configuration of a connection", response = ConnectionSettings::class)
    fun getConnectionConfigurations(
        @PathVariable("con") connection: String
    ): ConnectionSettings {
        return settings.getConnectionSettings(connection)
    }

    /**
     * Sends configurations of all connections
     *
     * @return Map of connection names/configurations
     */
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "Map of connections and configurations",
            response = ConnectionSettings::class,
            responseContainer = "Map"
        )
    )
    @ApiOperation(value = "Retrieves configurations of all connections")
    @GetMapping("/connectionConfigs", produces = [MediaType.APPLICATION_JSON])
    fun getAllConnectionConfigurations(): Map<String, ConnectionSettings> {
        try {
            val connectionManager = connectionManager ?: return emptyMap()

            // Set of all connection configurations, properly ordered
            val allSettings: MutableMap<String, ConnectionSettings> =
                TreeMap(
                    Comparator { o1: String, o2: String ->
                        when (Constants.GENERAL_CONFIG) {
                            o1 -> {
                                return@Comparator -1
                            }
                            o2 -> {
                                return@Comparator 1
                            }
                            else -> {
                                return@Comparator o1.compareTo(o2)
                            }
                        }
                    }
                )
            // Load all existing entries
            allSettings.putAll(settings.allConnectionSettings)
            // Assert global configuration entry
            allSettings.putIfAbsent(Constants.GENERAL_CONFIG, ConnectionSettings())
            val routeInputs =
                routeManager
                    .routes
                    .mapNotNull { it.id }
                    .associateWith { routeManager.getRouteInputUris(it) }

            // add all available endpoints
            for (endpoint in connectionManager.listAvailableEndpoints()) {
                // For every currently available endpoint, go through all preferences and check
                // if the id is already there. If not, create empty config.
                val hostIdentifier = endpoint.host + ":" + endpoint.port

                // create missing endpoint configurations
                if (allSettings.keys.stream().noneMatch { anObject: String? -> hostIdentifier == anObject }) {
                    allSettings[hostIdentifier] = ConnectionSettings()
                }
            }

            // add route id before host identifier for web console view
            val retAllSettings = mutableMapOf<String, ConnectionSettings>()
            allSettings.forEach { (key, value) ->
                if (key == Constants.GENERAL_CONFIG) {
                    retAllSettings[key] = value
                } else {
                    val endpointIdentifiers =
                        routeInputs
                            .entries
                            .filter { (_, value1) ->
                                value1.any { u: String -> u.startsWith("idsserver://$key") }
                            }
                            .map { "$it - $key" }
                            .ifEmpty { listOf("<no route found> - $key") }

                    // add endpoint configurations
                    endpointIdentifiers.forEach { endpointIdentifier: String ->
                        if (retAllSettings.keys.none { endpointIdentifier == it }) {
                            retAllSettings[endpointIdentifier] = value
                        }
                    }
                }
            }
            return retAllSettings
        } catch (e: Throwable) {
            e.printStackTrace()
            return emptyMap()
        }
    }

    companion object {
        private val CONNECTION_CONFIG_PATTERN = Pattern.compile(".* - ([^ ]+)$")
    }
}
