package de.fhg.camel.ids.connectionmanagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.camel.ids.server.ConnectorRef;
import de.fhg.camel.ids.server.DefaultWebsocket;
import de.fhg.camel.ids.server.MemoryWebsocketStore;
import de.fhg.camel.ids.server.WebsocketComponent;
import de.fhg.camel.ids.server.WebsocketComponentServlet;
import de.fhg.ids.comm.ws.protocol.ProtocolState;



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
	public List<IDSCPIncomingConnection> listIncomingConnections() {
		List<IDSCPIncomingConnection> connections = new ArrayList<IDSCPIncomingConnection>();
		
		Set<String> keySet = WebsocketComponent.CONNECTORS.keySet();
		Iterator<Entry<String, ConnectorRef>> it =  WebsocketComponent.CONNECTORS.entrySet().iterator();
	    while (it.hasNext()) {
	    	IDSCPIncomingConnection idscpc = new IDSCPIncomingConnection();
	    	
	        Map.Entry<String, ConnectorRef> pair = (Map.Entry<String, ConnectorRef>)it.next();
	        ConnectorRef connectorRef = pair.getValue();
	        MemoryWebsocketStore memoryStore = connectorRef.getMemoryStore();
	        Server server = connectorRef.getServer();
	        WebsocketComponentServlet servlet = connectorRef.getServlet();
	        idscpc.setEndpointIdentifier(servlet.getConsumer().getEndpoint().toString());
	        String protocol = servlet.getConsumer().getEndpoint().getProtocol();
	        Collection<DefaultWebsocket> websockets = memoryStore.getAll();
	        Iterator<DefaultWebsocket> webSocketIterator = websockets.iterator();
	        String protocolState;
	        //Assume only websocket per endpoint
	        while(webSocketIterator.hasNext())  {
	        	DefaultWebsocket dws = webSocketIterator.next();
	        	String connectionKey = dws.getConnectionKey();
	        	idscpc.setAttestationResult(dws.getCurrentProtocolState());
	        	
	        }
	        connections.add(idscpc);
	        it.remove(); // avoids a ConcurrentModificationException
	    }

		return connections;
	}
	
	@Override
	public List<IDSCPOutgoingConnection> listOutgoingConnections() {
		List<IDSCPOutgoingConnection> connections = new ArrayList<IDSCPOutgoingConnection>();
		
		Set<String> keySet = WebsocketComponent.CONNECTORS.keySet();
		Iterator<Entry<String, ConnectorRef>> it =  WebsocketComponent.CONNECTORS.entrySet().iterator();
	    while (it.hasNext()) {
	    	IDSCPOutgoingConnection idscpc = new IDSCPOutgoingConnection();
	    	
	        Map.Entry<String, ConnectorRef> pair = (Map.Entry<String, ConnectorRef>)it.next();
	        ConnectorRef connectorRef = pair.getValue();
	        MemoryWebsocketStore memoryStore = connectorRef.getMemoryStore();
	        Server server = connectorRef.getServer();
	        WebsocketComponentServlet servlet = connectorRef.getServlet();
	        idscpc.setEndpointIdentifier(servlet.getConsumer().getEndpoint().toString());
	        String protocol = servlet.getConsumer().getEndpoint().getProtocol();
	        
	        Collection<DefaultWebsocket> websockets = memoryStore.getAll();
	        Iterator<DefaultWebsocket> webSocketIterator = websockets.iterator();
	        
	        //Assume only websocket per endpoint
	        while(webSocketIterator.hasNext())  {
	        	DefaultWebsocket dws = webSocketIterator.next();
	        	String connectionKey = dws.getConnectionKey();
	        	
	        	// in order to check if the provider has done a successful remote attestation
	        	// we have to check the state of the dsm (=IDSCP_END) and the result of the rat:
	        	if(dws.getCurrentProtocolState().equals(ProtocolState.IDSCP_END.id()) && dws.isAttestationSuccessful()) {
	        		// attestation is done and was successful
	        		idscpc.setAttestationResult("Success");
	        		idscpc.setLastProtocolState(ProtocolState.IDSCP_END.id());
	        	}
	        	// in order to check the identity of the remote couterpart, we could use the SSLContextParameters of the dws:
	        	// this is "NONE", "WANT" or "REQUIRE"
	        	idscpc.setRemoteIdentity(dws.getRemoteHostname());
	        	idscpc.setRemoteAuthentication(dws.getSSLContextParameters().getServerParameters().getClientAuthentication());
	        	
	        	
	        }
	        connections.add(idscpc);
	        it.remove(); // avoids a ConcurrentModificationException
	    }

		return connections;
	}
	
}
