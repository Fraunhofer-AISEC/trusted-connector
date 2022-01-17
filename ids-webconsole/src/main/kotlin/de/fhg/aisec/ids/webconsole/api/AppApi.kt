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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.fhg.aisec.ids.api.cm.ApplicationContainer
import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.cm.NoContainerExistsException
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.webconsole.api.data.AppSearchRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.ServiceUnavailableException
import javax.ws.rs.client.ClientBuilder
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
@Component
@Path("app")
@Api(value = "Applications", authorizations = [Authorization(value = "oauth2")])
class AppApi {

    @Autowired private lateinit var cml: ContainerManager
    @Autowired private lateinit var settings: Settings

    @GET
    @Path("list")
    @ApiOperation(
        value = "List all applications installed in the connector",
        notes = "Returns an empty list if no apps are installed",
        response = ApplicationContainer::class,
        responseContainer = "List"
    )
    @ApiResponses(ApiResponse(code = 200, message = "List of apps"))
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
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

    @GET
    @Path("start/{containerId}")
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
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun start(
        @ApiParam(value = "ID of the app to start") @PathParam("containerId") containerId: String
    ): Boolean {
        return start(containerId, null)
    }

    @GET
    @Path("start/{containerId}/{key}")
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
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun start(
        @ApiParam(value = "ID of the app to start") @PathParam("containerId") containerId: String,
        @ApiParam(value = "Key for user token (required for trustX containers)") @PathParam("key") key: String?
    ): Boolean {
        return try {
            cml.startContainer(containerId, key)
            true
        } catch (e: NoContainerExistsException) {
            LOG.error("Error starting container", e)
            false
        } catch (e: ServiceUnavailableException) {
            LOG.error("Error starting container", e)
            false
        }
    }

    @GET
    @Path("stop/{containerId}")
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
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun stop(
        @ApiParam(value = "ID of the app to stop") @PathParam("containerId") containerId: String
    ): Boolean {
        return try {
            cml.stopContainer(containerId)
            true
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
            false
        } catch (e: ServiceUnavailableException) {
            LOG.error(e.message, e)
            false
        }
    }

    @POST // @OPTIONS
    @Path("install")
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
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun install(
        @ApiParam(value = "String with imageID", collectionFormat = "Map") apps: Map<String?, ApplicationContainer?>
    ): String {
        val app = apps["app"]
        LOG.debug("Request to load {}", app!!.image)
        val image = app.image
        if (image == null) {
            LOG.warn("Null image")
            throw InternalServerErrorException("Null image")
        }
        LOG.debug("Pulling app {}", image)
        CompletableFuture.supplyAsync {
            cml.pullImage(app)
        }.completeOnTimeout(null, PULL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        return "OK"
    }

    @GET
    @Path("wipe")
    @ApiOperation(value = "Wipes an app and all its data")
    @ApiResponses(
        ApiResponse(code = 200, message = "If the app is being wiped"),
        ApiResponse(code = 500, message = "_No cmld_ if no container management layer is available")
    )
    @AuthorizationRequired
    fun wipe(
        @ApiParam(value = "ID of the app to wipe") @QueryParam("containerId") containerId: String
    ): String {
        try {
            cml.wipe(containerId)
        } catch (e: NullPointerException) {
            LOG.error(e.message, e)
        } catch (e: NoContainerExistsException) {
            LOG.error(e.message, e)
        }
        return "OK"
    }

    @GET
    @Path("cml_version")
    @ApiOperation(
        value = "Returns the version of the currently active container management layer",
        response = MutableMap::class
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun getCml(): Map<String, String> {
        return try {
            val result: MutableMap<String, String> = HashMap()
            result["cml_version"] = cml.version
            result
        } catch (sue: ServiceUnavailableException) {
            emptyMap()
        }
    }

    @POST
    @Path("search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun search(searchRequest: AppSearchRequest): List<ApplicationContainer> {
        val term = searchRequest.searchTerm
        return try {
            val client = ClientBuilder.newBuilder().build()
            val url = settings.connectorConfig.appstoreUrl
            val r = client.target(url).request(MediaType.APPLICATION_JSON).get(
                String::class.java
            )
            val mapper = ObjectMapper()
            val result: List<ApplicationContainer> = mapper.readValue(
                r,
                object : TypeReference<List<ApplicationContainer>>() {}
            )
            if (term != "") {
                result
                    .parallelStream()
                    .filter { app: ApplicationContainer ->
                        (
                            app.name?.contains(term) ?: false ||
                                app.description?.contains(term) ?: false ||
                                app.image?.contains(term) ?: false ||
                                app.id?.contains(term) ?: false ||
                                app.categories.contains(term)
                            )
                    }
                    .collect(Collectors.toList())
            } else {
                result
            }
        } catch (e: IOException) {
            throw InternalServerErrorException(e)
        }
    }

    companion object {
        private const val PULL_TIMEOUT_MINUTES: Long = 20
        private val LOG = LoggerFactory.getLogger(AppApi::class.java)
    }
}
