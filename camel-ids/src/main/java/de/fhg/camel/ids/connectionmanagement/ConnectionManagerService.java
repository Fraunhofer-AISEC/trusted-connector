/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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
package de.fhg.camel.ids.connectionmanagement;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPEndpoint;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.camel.ids.server.DefaultWebsocket;
import de.fhg.camel.ids.server.MemoryWebsocketStore;
import de.fhg.camel.ids.server.WebsocketComponent;
import de.fhg.camel.ids.server.WebsocketComponent.ConnectorRef;
import de.fhg.camel.ids.server.WebsocketComponentServlet;

/**
 * Main entry point of the Connection Management Layer.
 *
 * This class is exposed as an OSGi Service and serves to access connection data from the management layer and REST API.
 *
 * @author Gerd Brost(gerd.brost@aisec.fraunhofer.de)
 *
 */
@Component(enabled=true, immediate=true, name="ids-conm")
public class ConnectionManagerService implements ConnectionManager {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectionManagerService.class);

	@Activate
	protected void activate() {
		LOG.info("Activating Connection Manager");
	}

	@Deactivate
	protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
		LOG.info("Deactivating Connection Manager");
	}

	@Override
	public List<IDSCPEndpoint> listAvailableEndpoints() {
		List<IDSCPEndpoint> endpoints = new ArrayList<IDSCPEndpoint>();
		
		Iterator<Entry<String, ConnectorRef>> it = WebsocketComponent.getConnectors().entrySet().iterator();
	    while (it.hasNext()) {
	    	IDSCPEndpoint endpoint = new IDSCPEndpoint();	
	        Map.Entry<String, ConnectorRef> mapEntry = it.next();
	        ConnectorRef connectorRef = mapEntry.getValue();
	        endpoint.setEndpointIdentifier(mapEntry.getValue().getServlet().getConsumer().getEndpoint().toString());
	        endpoints.add(endpoint);
	        
	        //TODO: Check behaviour. This is removed since the underlying collection is modified and thus the connection is really removed
	        //it.remove(); // avoids a ConcurrentModificationException
	    }

		return endpoints;
	}

	
	@Override
	public List<IDSCPIncomingConnection> listIncomingConnections() {
		List<IDSCPIncomingConnection> connections = new ArrayList<>();
		
		Iterator<Entry<String, ConnectorRef>> it = WebsocketComponent.getConnectors().entrySet().iterator();
	    while (it.hasNext()) {
	    	IDSCPIncomingConnection idscpc = new IDSCPIncomingConnection();
	    	
	        Map.Entry<String, ConnectorRef> pair = it.next();
	        ConnectorRef connectorRef = pair.getValue();
	        MemoryWebsocketStore memoryStore = connectorRef.getMemoryStore();
	        WebsocketComponentServlet servlet = connectorRef.getServlet();
	        idscpc.setEndpointIdentifier(servlet.getConsumer().getEndpoint().toString());
	        Collection<DefaultWebsocket> websockets = memoryStore.getAll();
	        Iterator<DefaultWebsocket> webSocketIterator = websockets.iterator();

	        //Assume only one websocket per endpoint
	        while(webSocketIterator.hasNext())  {
	        	DefaultWebsocket dws = webSocketIterator.next();
	        	idscpc.setAttestationResult(dws.getAttestationResult());
	        }
	        connections.add(idscpc);
	        //TODO: Check behaviour. This is removed since the underlying collection is modified and thus the connection is really removed
	        //it.remove(); // avoids a ConcurrentModificationException
	    }

		return connections;
	}
	
	@Override
	public List<IDSCPOutgoingConnection> listOutgoingConnections() {
		List<IDSCPOutgoingConnection> connections = new ArrayList<>();
		
		Iterator<Entry<String, ConnectorRef>> it =  WebsocketComponent.getConnectors().entrySet().iterator();
	    while (it.hasNext()) {
	    	IDSCPOutgoingConnection idscpc = new IDSCPOutgoingConnection();
	    	
	        Map.Entry<String, ConnectorRef> pair = it.next();
	        ConnectorRef connectorRef = pair.getValue();
	        MemoryWebsocketStore memoryStore = connectorRef.getMemoryStore();
	        WebsocketComponentServlet servlet = connectorRef.getServlet();
	        idscpc.setEndpointIdentifier(servlet.getConsumer().getEndpoint().toString());
	        
	        Collection<DefaultWebsocket> websockets = memoryStore.getAll();
	        Iterator<DefaultWebsocket> webSocketIterator = websockets.iterator();
	        
	        //Assume only one websocket per endpoint
	        while(webSocketIterator.hasNext())  {
	        	DefaultWebsocket dws = webSocketIterator.next();
	        	
	        	// in order to check if the provider has done a successful remote attestation
	        	// we have to check the state of the dsm (=IDSCP_END) and the result of the rat:
	        	idscpc.setAttestationResult(dws.getAttestationResult());
	        	idscpc.setLastProtocolState(dws.getCurrentProtocolState());

	        	// in order to check the identity of the remote counterpart, we could use the SSLContextParameters of the dws:
	        	// this is "NONE", "WANT" or "REQUIRE"
	        	SSLContextParameters sslParams = connectorRef.getServlet().getConsumer().getEndpoint().getSslContextParameters();
	        	idscpc.setRemoteIdentity(dws.getRemoteHostname());
	        	idscpc.setRemoteAuthentication(sslParams.getServerParameters().getClientAuthentication());
	        	
	        	
	        }
	        connections.add(idscpc);
	        //TODO: Check behaviour. This is removed since the underlying collection is modified and thus the connection is really removed
	        //it.remove(); // avoids a ConcurrentModificationException
	    }

		return connections;
	}	
}