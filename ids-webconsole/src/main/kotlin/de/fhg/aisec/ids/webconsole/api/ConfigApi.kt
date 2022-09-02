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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.TreeMap
import java.util.function.Consumer
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.ws.rs.BadRequestException
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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
@Component
@Path("/config")
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

    @GET
    @ApiOperation(value = "Retrieves the current configuration", response = ConnectorConfig::class)
    @Path("/connectorConfig")
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun get(): ConnectorConfig {
        return settings.connectorConfig
    }

    @POST
    // @OPTIONS
    @Path("/connectorConfig")
    @ApiOperation(value = "Sets the overall configuration of the connector")
    @ApiResponses(
        ApiResponse(
            code = 500,
            message = "_No valid preferences received_: If incorrect configuration parameter is provided"
        )
    )
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun set(config: ConnectorConfig?): String {
        if (config == null) {
            throw BadRequestException("No valid preferences received!")
        }
        settings.connectorConfig = config
        return "OK"
    }

    /**
     * Save connection configuration of a particular connection.
     *
     * @param connection The name of the connection
     * @param conSettings The connection configuration of the connection
     */
    @POST
    @Path("/connectionConfigs/{con}")
    @ApiOperation(value = "Save connection configuration of a particular connection")
    @ApiResponses(
        ApiResponse(
            code = 500,
            message = "_No valid connection settings received!_: If incorrect connection settings parameter is provided"
        )
    )
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun setConnectionConfigurations(
        @PathParam("con") connection: String,
        conSettings: ConnectionSettings?
    ): Response {
        return conSettings?.let {
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
            Response.ok().build()
        } ?: Response.serverError().entity("No valid connection settings received!").build()
    }

    /**
     * Sends configuration of a connection
     *
     * @param connection Connection identifier
     * @return The connection configuration of the requested connection
     */
    @GET
    @Path("/connectionConfigs/{con}")
    @ApiOperation(value = "Sends configuration of a connection", response = ConnectionSettings::class)
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun getConnectionConfigurations(@PathParam("con") connection: String): ConnectionSettings {
        return settings.getConnectionSettings(connection)
    } // add endpoint configurations// For every currently available endpoint, go through all preferences and check
    // if the id is already there. If not, create empty config.

    // create missing endpoint configurations

    // add route id before host identifier for web console view
// Set of all connection configurations, properly ordered
    // Load all existing entries
    // Assert global configuration entry

    // add all available endpoints
    /**
     * Sends configurations of all connections
     *
     * @return Map of connection names/configurations
     */
    @get:AuthorizationRequired
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:ApiResponses(
        ApiResponse(
            code = 200,
            message = "Map of connections and configurations",
            response = ConnectionSettings::class,
            responseContainer = "Map"
        )
    )
    @get:ApiOperation(value = "Retrieves configurations of all connections")
    @get:Path("/connectionConfigs")
    @get:GET
    val allConnectionConfigurations: Map<String, ConnectionSettings>
        get() {
            val connectionManager = connectionManager ?: return emptyMap()

            // Set of all connection configurations, properly ordered
            val allSettings: MutableMap<String, ConnectionSettings> = TreeMap(
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
            val routeInputs = routeManager
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
            val retAllSettings: MutableMap<String, ConnectionSettings> = HashMap()
            for ((key, value) in allSettings) {
                if (key == Constants.GENERAL_CONFIG) {
                    retAllSettings[key] = value
                } else {
                    var endpointIdentifiers = routeInputs
                        .entries
                        .stream()
                        .filter { (_, value1) ->
                            value1.stream().anyMatch { u: String -> u.startsWith("idsserver://$key") }
                        }
                        .map { (key1) -> "$key1 - $key" }
                        .collect(Collectors.toList())
                    if (endpointIdentifiers.isEmpty()) {
                        endpointIdentifiers = listOf("<no route found> - $key")
                    }

                    // add endpoint configurations
                    endpointIdentifiers.forEach(
                        Consumer { endpointIdentifier: String ->
                            if (retAllSettings.keys.stream()
                                .noneMatch { anObject: String? -> endpointIdentifier == anObject }
                            ) {
                                retAllSettings[endpointIdentifier] = value
                            }
                        }
                    )
                }
            }
            return retAllSettings
        }

    companion object {
        private val CONNECTION_CONFIG_PATTERN = Pattern.compile(".* - ([^ ]+)$")
    }
}
