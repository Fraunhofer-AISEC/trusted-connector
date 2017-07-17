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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for managing connections from and to the connector.
 * 
 * The API will be available at st<method>.
 *                              
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
@Path("/incomingconnections")
public class ConnectionAPI {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionAPI.class);
	
	@GET
	@Path("listincoming")
	@Produces("application/json")
	public String listincoming() {
		List<IDSCPIncomingConnection> result = new ArrayList<>();
		
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		if (connectionManager.isPresent()) {
			result = connectionManager.get().listIncomingConnections();
		}
		
		return new GsonBuilder().create().toJson(result);
	}
	
	@GET
	@Path("listoutgoing")
	@Produces("application/json")
	public String listoutgoing() {
		List<IDSCPOutgoingConnection> result = new ArrayList<>();
		
		Optional<ConnectionManager> connectionManager = WebConsoleComponent.getConnectionManager();
		if (connectionManager.isPresent()) {
			result = connectionManager.get().listOutgoingConnections();
		}
		
		return new GsonBuilder().create().toJson(result);
	}

}

