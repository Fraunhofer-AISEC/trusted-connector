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

import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Authorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing connections from and to the connector.
 *
 *
 * The API will be available at st/<method>.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
</method> */
@Component
@Path("/connections")
@Api(value = "IDSCP Connections", authorizations = [Authorization(value = "oauth2")])
class ConnectionAPI {

    @Autowired
    private lateinit var connectionManager: ConnectionManager

    @get:AuthorizationRequired
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:ApiOperation(
        value = "Returns a list of all inbound connections",
        response = IDSCPIncomingConnection::class,
        responseContainer = "List"
    )
    @get:Path("/incoming")
    @get:GET
    val incoming: List<IDSCPIncomingConnection>
        get() = connectionManager.listIncomingConnections() ?: emptyList()

    @get:AuthorizationRequired
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:ApiOperation(
        value = "Returns a list of all outbound connections",
        response = IDSCPOutgoingConnection::class,
        responseContainer = "List"
    )
    @get:Path("/outgoing")
    @get:GET
    val outgoing: List<IDSCPOutgoingConnection>
        get() = connectionManager.listOutgoingConnections() ?: emptyList()

    @AuthorizationRequired
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Returns a list of all endpoints provided by this connector",
        response = IDSCPServerEndpoint::class,
        responseContainer = "List"
    )
    @Path("/endpoints")
    @GET
    // val availableEndpoints: List<IDSCPServerEndpoint>
    //    get() = connectionManager.listAvailableEndpoints()
    fun listendpoints(): List<IDSCPServerEndpoint> {
        return connectionManager.listAvailableEndpoints()
    }
}
