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
import de.fhg.aisec.ids.api.router.RouteComponent
import de.fhg.aisec.ids.api.router.RouteManager
import de.fhg.aisec.ids.api.router.RouteObject
import de.fhg.aisec.ids.webconsole.api.data.ValidationInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
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
@Component
@Path("/routes")
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
    @GET
    @Path("list")
    @ApiOperation(
        value = "Returns map from camel context to list of camel routes.",
        response = RouteObject::class,
        responseContainer = "List"
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun list(): List<RouteObject> {
        return rm.routes
    }

    @GET
    @Path("/get/{id}")
    @ApiOperation(value = "Get a Camel route", response = RouteObject::class)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    operator fun get(@ApiParam(value = "Route ID") @PathParam("id") id: String): RouteObject {
        return rm.getRoute(id) ?: throw NotFoundException("Route not found")
    }

    /** Stop a route based on an id.  */
    @GET
    @Path("/startroute/{id}")
    @ApiOperation(value = "Starts a Camel route. The route will start to process messages.")
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun startRoute(@PathParam("id") id: String?): Result {
        return try {
            rm.startRoute(id!!)
            Result()
        } catch (e: Exception) {
            LOG.warn(e.message, e)
            Result(false, e.message!!)
        }
    }

    /** Stop a route based on its id.  */
    @GET
    @Path("/stoproute/{id}")
    @ApiOperation(value = "Stops a Camel route. The route will remain installed but it will not process any messages.")
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun stopRoute(@PathParam("id") id: String?): Result {
        return try {
            rm.stopRoute(id!!)
            Result()
        } catch (e: Exception) {
            LOG.warn(e.message, e)
            Result(false, e.message!!)
        }
    }

    /**
     * Retrieve list of supported components (aka protocols which can be addressed by Camel)
     *
     * @return List of supported protocols
     */
    @get:AuthorizationRequired
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:Path("/components")
    @get:GET
    val components: List<RouteComponent>
        get() = rm.listComponents()

    /** Retrieve list of currently installed endpoints (aka URIs to/from which routes exist)  */
    @GET
    @Path("/list_endpoints")
    @AuthorizationRequired
    fun listEndpoints(): Map<String, String> {
        return rm.listEndpoints()
    }

    @GET
    @Path("/validate/{routeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationRequired
    fun validate(@PathParam("routeId") routeId: String?): ValidationInfo {
        val pap: PAP = policyAdministrationPoint
            ?: throw InternalServerErrorException()
        val rvp = pap.verifyRoute(routeId!!) ?: throw InternalServerErrorException()
        val vi = ValidationInfo()
        vi.valid = rvp.isValid
        if (!rvp.isValid) {
            vi.counterExamples = rvp.counterExamples
        }
        return vi
    }

    @GET
    @Path("/prolog/{routeId}")
    @Produces(MediaType.TEXT_PLAIN)
    @AuthorizationRequired
    fun getRouteProlog(@PathParam("routeId") routeId: String): String {
        return rm.getRouteAsProlog(routeId)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RouteApi::class.java)
    }
}
