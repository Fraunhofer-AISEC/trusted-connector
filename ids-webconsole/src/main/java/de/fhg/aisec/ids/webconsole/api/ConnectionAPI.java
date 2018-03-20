/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for managing connections from and to the connector.
 * 
 * The API will be available at st/<method>.
 *                              
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
@Path("/connections")
public class ConnectionAPI {
	
	@GET
	@Path("/incoming")
	@Produces(MediaType.APPLICATION_JSON)
	public List<IDSCPIncomingConnection> getIncoming() {
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		if (connectionManager.isPresent()) {
			return connectionManager.get().listIncomingConnections();
		} else {
			return Collections.emptyList();
		}
	}
	
	@GET
	@Path("/outgoing")
	@Produces(MediaType.APPLICATION_JSON)
	public List<IDSCPOutgoingConnection> getOutgoing() {
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		if (connectionManager.isPresent()) {
			return connectionManager.get().listOutgoingConnections();
		} else {
			return Collections.emptyList();
		}
	}
	
	@GET
	@Path("/endpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public List<IDSCPServerEndpoint> getAvailableEndpoints() {
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		if (connectionManager.isPresent()) {
			return connectionManager.get().listAvailableEndpoints();
		} else {
			return Collections.emptyList();
		}
	}
	
	
}

