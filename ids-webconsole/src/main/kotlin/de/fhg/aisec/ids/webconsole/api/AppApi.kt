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

import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.cm.NoContainerExistsException
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.ApiController
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.jackson
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing "apps" in the connector.
 *
 *
 * In this implementation, apps are either docker or trustX containers.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/apps/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
</method> */

@ApiController
@RequestMapping("/app")
@Api(value = "Applications", authorizations = [Authorization(value = "oauth2")])
class AppApi {

    @Autowired
    private lateinit var cml: ContainerManager

    @Autowired
    private lateinit var settings: Settings

    @GetMapping("list", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "List all applications installed in the connector",
        notes = "Returns an empty list if no apps are installed",
        response = ApplicationContainer::class,
        responseContainer = "List"
    )
    @ApiResponses(ApiResponse(code = 200, message = "List of apps"))
    fun list(): List<ApplicationContainer> {
        return cml.list(false).sortedWith(
            java.util.Comparator { app1: ApplicationContainer, app2: ApplicationContainer ->
                try {
                    val date1 = ZonedDateTime.parse(app1.created)
                    val date2 = ZonedDateTime.parse(app2.created)
                    return@Comparator date1.compareTo(date2)
                } catch (t: Exception) {
                    LOG.warn("Unexpected app creation date/time. Cannot sort. {}", t.message)
                }
                0
            }
        )
    }

    @GetMapping("start/{containerId}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Start an application",
        notes = "Starting an application may take some time. " +
            "This method will start the app asynchronously and return immediately. " +
            "This method starts the latest version of the app.",
        response = Boolean::class
    )
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "true if the app has been requested to be started. " +
                "false if no container management layer is available"
        )
    )
    fun start(
        @ApiParam(value = "ID of the app to start")
        @PathVariable("containerId")
        containerId: String
    ): Boolean {
        return start(containerId, null)
    }

    @GetMapping("start/{containerId}/{key}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Start an application",
        notes = "Starting an application may take some time. This method will start the app asynchronously and return immediately. This methods starts a specific version of the app.",
        response = Boolean::class
    )
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "true if the app has been requested to be started. " +
                "false if no container management layer is available"
        )
    )
    fun start(
        @ApiParam(value = "ID of the app to start")
        @PathVariable("containerId")
        containerId: String,
        @ApiParam(value = "Key for user token (required for trustX containers)")
        @PathVariable("key")
        key: String?
    ): Boolean {
        return try {
            cml.startContainer(containerId, key)
            true
        } catch (e: NoContainerExistsException) {
            LOG.error("Error starting container", e)
            false
        }
    }

    @GetMapping("stop/{containerId}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Stop an app",
        notes = "Stops an application. The application will remain installed and can be re-started later. All temporary data will be lost, however.",
        response = Boolean::class
    )
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "true if the app has been requested to be stopped. " +
                "false if no container management layer is available"
        )
    )
    fun stop(
        @ApiParam(value = "ID of the app to stop")
        @PathVariable("containerId")
        containerId: String
    ): Boolean {
        return try {
            cml.stopContainer(containerId)
            true
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
            false
        }
    }

    @PostMapping("install", consumes = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Install an app", notes = "Requests to install an app.", response = Boolean::class)
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "If the app has been requested to be installed. " +
                "The actual installation takes place asynchronously in the background " +
                "and will terminate after a timeout of 20 minutes",
            response = Boolean::class
        ),
        ApiResponse(
            code = 500,
            message = "_No cmld_: If no container management layer is available",
            response = String::class
        ),
        ApiResponse(code = 500, message = "_Null image_: If imageID not given", response = String::class)
    )
    fun install(
        @ApiParam(
            value = "String with imageID",
            collectionFormat = "Map"
        )
        @RequestBody
        app: ApplicationContainer
    ) {
        LOG.debug("Request to load {}", app.image)
        val image = app.image
        if (image == null) {
            LOG.warn("Null image")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Null image")
        }
        LOG.debug("Pulling app {}", image)
        CoroutineScope(Dispatchers.IO).launch {
            cml.pullImage(app)
        }
    }

    @GetMapping("wipe")
    @ApiOperation(value = "Wipes an app and all its data")
    @ApiResponses(
        ApiResponse(code = 200, message = "If the app is being wiped"),
        ApiResponse(code = 500, message = "_No cmld_ if no container management layer is available")
    )
    fun wipe(
        @ApiParam(value = "ID of the app to wipe")
        @RequestParam
        containerId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cml.wipe(containerId)
            } catch (e: Throwable) {
                LOG.error(e.message, e)
            }
        }
    }

    @GetMapping("cml_version", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Returns the version of the currently active container management layer",
        response = MutableMap::class
    )
    fun getCml(): Map<String, String> {
        return try {
            val result: MutableMap<String, String> = HashMap()
            result["cml_version"] = cml.version
            result
        } catch (sue: Exception) {
            emptyMap()
        }
    }

    @PostMapping(
        "search",
        consumes = [MediaType.TEXT_PLAIN],
        produces = [MediaType.APPLICATION_JSON]
    )
    suspend fun search(@RequestBody term: String?): List<ApplicationContainer> {
        return httpClient.get(settings.connectorConfig.appstoreUrl).body<List<ApplicationContainer>>().let { res ->
            if (term?.isNotBlank() == true) {
                res.filter { app: ApplicationContainer ->
                    app.name?.contains(term, true) ?: false ||
                        app.description?.contains(term, true) ?: false ||
                        app.image?.contains(term, true) ?: false ||
                        app.id?.contains(term, true) ?: false ||
                        app.categories.any { it.contains(term, true) }
                }
            } else {
                res
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AppApi::class.java)

        private val httpClient = HttpClient(Java) {
            install(ContentNegotiation) {
                jackson(ContentType.Any)
            }
        }
    }
}
