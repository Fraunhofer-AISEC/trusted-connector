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
import de.fhg.aisec.ids.api.conm.ServerEndpoint
import de.fhg.aisec.ids.webconsole.ApiController
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Authorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing connections from and to the connector.
 *
 *
 * The API will be available at st/<method>.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
</method> */
@ApiController
@RequestMapping("/connections")
@Api(value = "IDSCP Connections", authorizations = [Authorization(value = "oauth2")])
class ConnectionApi {
    @Autowired
    private lateinit var connectionManager: ConnectionManager

    @ApiOperation(
        value = "Returns a list of all inbound connections",
        response = IDSCPIncomingConnection::class,
        responseContainer = "List"
    )
    @GetMapping("/incoming", produces = [MediaType.APPLICATION_JSON])
    fun getIncomingConnections() = connectionManager.listIncomingConnections()

    @ApiOperation(
        value = "Returns a list of all outbound connections",
        response = IDSCPOutgoingConnection::class,
        responseContainer = "List"
    )
    @GetMapping("/outgoing", produces = [MediaType.APPLICATION_JSON])
    fun getOutgoingConnections() = connectionManager.listOutgoingConnections()

    @ApiOperation(
        value = "Returns a list of all endpoints provided by this connector",
        response = ServerEndpoint::class,
        responseContainer = "List"
    )
    @GetMapping("/endpoints", produces = [MediaType.APPLICATION_JSON])
    fun availableEndpoints(): List<ServerEndpoint> {
        return connectionManager.listAvailableEndpoints()
    }
}
