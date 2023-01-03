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

import de.fhg.aisec.ids.api.Result
import de.fhg.aisec.ids.api.policy.PAP
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.api.router.RouteObject
import de.fhg.aisec.ids.webconsole.ApiController
import de.fhg.aisec.ids.webconsole.api.data.ValidationInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import javax.ws.rs.core.MediaType

/**
 * REST API interface for "data pipes" in the connector.
 *
 *
 * This implementation uses Camel Routes as data pipes, i.e. the API methods allow inspection of
 * camel routes in different camel contexts.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/routes/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
</method> */
@ApiController
@RequestMapping("/routes")
@Api(value = "Message Routing", authorizations = [Authorization(value = "oauth2")])
class RouteApi {

    @Autowired
    private lateinit var rm: RouteManager

    @Autowired(required = false)
    private var policyAdministrationPoint: PAP? = null

    /**
     * Returns map from camel context to list of camel routes.
     *
     *
     * Example:
     *
     *
     * {"camel-1":["Route(demo-route)[[From[timer://simpleTimer?period\u003d10000]] -\u003e
     * [SetBody[simple{This is a demo body!}], Log[The message contains ${body}]]]"]}
     *
     * @return The resulting route objects
     */
    @GetMapping("/list", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(
        value = "Returns map from camel context to list of camel routes.",
        response = RouteObject::class,
        responseContainer = "List"
    )
    fun list(): List<RouteObject> {
        return rm.routes
    }

    @GetMapping("/get/{id}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Get a Camel route", response = RouteObject::class)
    operator fun get(
        @ApiParam(value = "Route ID")
        @PathVariable("id")
        id: String
    ): RouteObject {
        return rm.getRoute(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found")
    }

    /** Stop a route based on an id.  */
    @GetMapping("/startroute/{id}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Starts a Camel route. The route will start to process messages.")
    fun startRoute(@PathVariable("id") id: String): Result {
        return try {
            rm.startRoute(id)
            Result()
        } catch (e: Exception) {
            LOG.warn(e.message, e)
            e.message?.let { Result(false, it) } ?: Result(false)
        }
    }

    /** Stop a route based on its id.  */
    @GetMapping("/stoproute/{id}", produces = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Stops a Camel route. The route will remain installed but it will not process any messages.")
    fun stopRoute(@PathVariable("id") id: String): Result {
        return try {
            rm.stopRoute(id)
            Result()
        } catch (e: Exception) {
            LOG.warn(e.message, e)
            e.message?.let { Result(false, it) } ?: Result(false)
        }
    }

    /**
     * Retrieve list of supported components (aka protocols which can be addressed by Camel)
     *
     * @return List of supported protocols
     */
    @GetMapping("/components", produces = [MediaType.APPLICATION_JSON])
    fun getComponents() = rm.listComponents()

    /** Retrieve list of currently installed endpoints (aka URIs to/from which routes exist)  */
    @GetMapping("/list_endpoints", produces = [MediaType.APPLICATION_JSON])
    fun listEndpoints(): Map<String, String> {
        return rm.listEndpoints()
    }

    @GetMapping("/validate/{routeId}", produces = [MediaType.APPLICATION_JSON])
    fun validate(@PathVariable("routeId") routeId: String): ValidationInfo {
        val pap: PAP = policyAdministrationPoint
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val rvp = pap.verifyRoute(routeId) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val vi = ValidationInfo()
        vi.valid = rvp.isValid
        if (!rvp.isValid) {
            vi.counterExamples = rvp.counterExamples
        }
        return vi
    }

    @GetMapping("/prolog/{routeId}", produces = [MediaType.TEXT_PLAIN])
    fun getRouteProlog(@PathVariable("routeId") routeId: String): String {
        return rm.getRouteAsProlog(routeId)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RouteApi::class.java)
    }
}
