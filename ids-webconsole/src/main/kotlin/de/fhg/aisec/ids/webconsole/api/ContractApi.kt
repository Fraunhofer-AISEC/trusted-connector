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

import de.fhg.aisec.ids.api.settings.Settings
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Component
@Path("/user")
@Api(value = "User Authentication")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ContractApi(@Autowired private val settings: Settings) {

    @DELETE
    @Path("/removeUser/{user}")
    @AuthorizationRequired
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun removeUser(@PathParam("user") username: String) = settings.removeUser(username)

    @GET
    @Path("list_user_names")
    @ApiOperation(value = "Lists user names", responseContainer = "List")
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "List of user names",
            response = String::class,
            responseContainer = "List"
        )
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun listUsersNames(): List<String> = settings.getUsers().keys.toList()

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractApi::class.java)
    }
}
