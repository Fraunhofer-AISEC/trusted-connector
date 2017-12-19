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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPClientEndpoint;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.camel.ids.client.WsEndpoint;
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
	public List<IDSCPServerEndpoint> listAvailableEndpoints() {
		List<IDSCPServerEndpoint> endpoints = new ArrayList<IDSCPServerEndpoint>();
		
		Iterator<Entry<String, ConnectorRef>> connectorIterator = WebsocketComponent.getConnectors().entrySet().iterator();
	    while (connectorIterator.hasNext()) {
	    	IDSCPServerEndpoint idscpendpoint = new IDSCPServerEndpoint();	
	        Map.Entry<String, ConnectorRef> mapEntry = connectorIterator.next();
	        ConnectorRef connectorRef = mapEntry.getValue();

	        idscpendpoint.setHost(connectorRef.getConnector().getHost());
	        idscpendpoint.setPort(Integer.toString(connectorRef.getConnector().getPort()));
	        idscpendpoint.setDefaultProtocol(connectorRef.getConnector().getDefaultProtocol());
	        idscpendpoint.setEndpointIdentifier(connectorRef.getConnector().toString());
	        endpoints.add(idscpendpoint);

	    }

		return endpoints;
	}

	
	@Override
	public List<IDSCPIncomingConnection> listIncomingConnections() {
		List<IDSCPIncomingConnection> connections = new ArrayList<>();
		
		Iterator<Entry<String, ConnectorRef>> connectorIterator = WebsocketComponent.getConnectors().entrySet().iterator();
	    while (connectorIterator.hasNext()) {
	        Map.Entry<String, ConnectorRef> pair = connectorIterator.next();
	        ConnectorRef connectorRef = pair.getValue();
	        MemoryWebsocketStore memoryStore = connectorRef.getMemoryStore();
	        WebsocketComponentServlet servlet = connectorRef.getServlet();
	        
	        //Servlet only present if an incoming connection exists. If null, do not collect consumer information. 
	        if(servlet != null) {
	        	Collection<DefaultWebsocket> websockets = memoryStore.getAll();
	        	Iterator<DefaultWebsocket> webSocketIterator = websockets.iterator();
	
		        //Every connection has a websocket. We collect connection information this way. 
		        while(webSocketIterator.hasNext())  {
		        	DefaultWebsocket dws = webSocketIterator.next();
			    	IDSCPIncomingConnection incomingConnection = new IDSCPIncomingConnection();
			    	incomingConnection.setEndpointIdentifier(servlet.getConsumer().getEndpoint().toString());
			    	incomingConnection.setConnectionKey(dws.getConnectionKey());
			    	incomingConnection.setRemoteHostName(dws.getRemoteHostname());
		        	incomingConnection.setAttestationResult(dws.getAttestationResult());
		        	connections.add(incomingConnection);
		        }
	        }
	    }

		return connections;
	}
	
	@Override
	public List<IDSCPOutgoingConnection> listOutgoingConnections() {
		List<IDSCPOutgoingConnection> connections = new ArrayList<>();
		List<IDSCPClientEndpoint> clientEndpoints = WsEndpoint.getEndpointList();
		
        Iterator<IDSCPClientEndpoint> clientEndpointsIterator = clientEndpoints.iterator();
        
        //TODO: This is still buggy and runs forever.
        //Iterate over the websockets. For every outgoing connection, a web socket is maintained. 
        while(clientEndpointsIterator.hasNext())  {
        	IDSCPClientEndpoint wse = clientEndpointsIterator.next();
	    	IDSCPOutgoingConnection outgoingConnection = new IDSCPOutgoingConnection();
	    	
	    	outgoingConnection.setEndpointIdentifier(wse.getEndpointIdentifier());
	    	outgoingConnection.setAttestationResult(wse.getAttestationResult());
        	// in order to check if the provider has done a successful remote attestation
        	// we have to check the state of the dsm (=IDSCP_END) and the result of the rat:
        	
        	connections.add(outgoingConnection);
        	
        }

        return connections;
	}	
	

}