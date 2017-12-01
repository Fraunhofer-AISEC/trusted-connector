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
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
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
	@Path("listincoming")
	@Produces("application/json")
	public List<IDSCPIncomingConnection> listincoming() {
		List<IDSCPIncomingConnection> result = new ArrayList<>();
//		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
//		if (connectionManager.isPresent()) {
//			result = connectionManager.get().listIncomingConnections();
//		}
		IDSCPIncomingConnection idsc = new IDSCPIncomingConnection();
		idsc.setAttestationResult(AttestationResult.SUCCESS);
		idsc.setEndpointIdentifier("EndpointIdentifier");
		result.add(idsc);
		return result;
	}
	
	@GET
	@Path("listoutgoing")
	@Produces("application/json")
	public List<IDSCPOutgoingConnection> listoutgoing() {
		List<IDSCPOutgoingConnection> result = new ArrayList<>();		
//		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
//		if (connectionManager.isPresent()) {
//			result = connectionManager.get().listOutgoingConnections();
//		}
		IDSCPOutgoingConnection idsc = new IDSCPOutgoingConnection();
		idsc.setEndpointIdentifier("EndpointID");
		idsc.setAttestationResult(AttestationResult.SUCCESS);
		idsc.setLastProtocolState("LastProtState");
		idsc.setRemoteAuthentication("RemoteAuth");
		idsc.setRemoteIdentity("RemoteIdentity");
		result.add(idsc);
		return result;
	}
}

