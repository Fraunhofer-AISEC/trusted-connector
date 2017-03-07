/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.server;

import java.io.Serializable;
import java.net.URI;
import java.util.UUID;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage.Type;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

@WebSocket
public class DefaultWebsocket implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DefaultWebsocket.class);

    private final WebsocketConsumer consumer;
    private final NodeSynchronization sync;
    private ProtocolMachine machine;
    private Session session;
    private String connectionKey;
	private FSM idsFsm;

    public DefaultWebsocket(NodeSynchronization sync, WebsocketConsumer consumer) {
        this.sync = sync;
        this.consumer = consumer;
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String message) {
        //LOG.trace("onClose {} {}", closeCode, message);
        sync.removeSocket(this);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        //LOG.trace("onConnect {}", session);
        this.session = session;
        this.connectionKey = UUID.randomUUID().toString();
        IdsAttestationType type;
        SSLContextParameters params = this.consumer.getSSLContextParameters();
        int attestationMask = 0;
        switch(this.consumer.getAttestationType()) {
	    	case 0:            
	    		type = IdsAttestationType.BASIC;
	    		break;
	    	case 1:
	    		type = IdsAttestationType.ALL;
	    		break;
	    	case 2:
	    		type = IdsAttestationType.ADVANCED;
	    		attestationMask = this.consumer.getAttestationMask();
	    		break;
	    	case 3:
	    		type = IdsAttestationType.ZERO;
	    		break;
	    	default:
	    		type = IdsAttestationType.BASIC;
	    		break;
        }
		// Integrate server-side of IDS protocol
        machine = new ProtocolMachine();
        idsFsm = machine.initIDSProviderProtocol(session, type, attestationMask, params);
        sync.addSocket(this);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        
        // Check if fsm is in its final state and successful. Only then, the message is forwarded to Camel consumer
        if (idsFsm.getState().equals(ProtocolState.IDSCP_END.id())) {
	        if (this.consumer != null) {
	            this.consumer.sendMessage(this.connectionKey, message);
	        } else {
	            //LOG.debug("No consumer to handle message received: {}", message);
	        }
	        return;
        }

        // Otherwise, we are still in the process of running IDS protocol and hold back the original message. In this case, feed the message into the protocol FSM
        try {
        	ConnectorMessage msg = ConnectorMessage.parseFrom(message.getBytes());
        	//LOG.debug("Feeding message into provider fsm: " + message);
        	//we de-protobuf and split messages into cmd and payload
        	idsFsm.feedEvent(new Event(msg.getType(), message, msg));
		} catch (InvalidProtocolBufferException e) {
			// An invalid message has been received during IDS protocol. close connection
			e.printStackTrace();
			this.session.close(new CloseStatus(403, "invalid protobuf"));
		}
        
    }


    @OnWebSocketMessage
    public void onMessage(byte[] data, int offset, int length) {
        //LOG.debug("server received onMessage " + new String(data));
        if (idsFsm.getState().equals(ProtocolState.IDSCP_END.id())) {
        	System.out.println("Successfully finished IDSCP");
	        if (this.consumer != null) {
	        	if(machine.getIDSCPProviderSuccess()) {
	        		this.consumer.sendMessage(this.connectionKey, data);
	        	}
	        	else {
	        		LOG.debug("remote attestation was NOT successful ... ");
	        		// do stuff when attestation was not successful here
	        	}
	        } 
	        else {
	            LOG.debug("No consumer to handle message received: {}", data);
	        }
        } 
        else {
			try {
				ConnectorMessage msg = ConnectorMessage.parseFrom(data);
	        	//System.out.println("Feeding message into provider fsm: " + data);
	        	idsFsm.feedEvent(new Event(msg.getType(), new String(data), msg));	//we need to de-protobuf here and split messages into cmd and payload
			} catch (InvalidProtocolBufferException e) {
				// An invalid message has been received during IDS protocol. close connection
				e.printStackTrace();
				this.session.close(new CloseStatus(403, "invalid protobuf"));
			}
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getConnectionKey() {
        return connectionKey;
    }

    public void setConnectionKey(String connectionKey) {
        this.connectionKey = connectionKey;
    }
}