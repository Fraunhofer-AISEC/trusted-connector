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
package de.fhg.ids.comm.server;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import de.fhg.aisec.ids.api.conm.AttestationResult;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.ws.protocol.ProtocolMachine;
import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

/**
 * Handles messages for the IDS protocol.
 * 
 * Messages from and to the web socket are connected to the FSM implementing the actual protocol.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@WebSocket
public class IdspServerSocket {
    private Logger LOG = LoggerFactory.getLogger(IdspServerSocket.class);
    private FSM fsm;
    private int attestationType = 0;
    private int attestationMask = 0;
    private ProtocolMachine machine;
    private boolean ratSuccess = false;
    private SSLContextParameters params;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition isFinishedCond = lock.newCondition();
    private final ConnectorMessage startMsg = Idscp.ConnectorMessage
										    		.newBuilder()
										    		.setType(ConnectorMessage.Type.RAT_START)
										    		.setId(new java.util.Random().nextLong())
										    		.build();
	
	public IdspServerSocket() {
		// Create provider socket
		this.attestationMask = 0;
		this.attestationType = 0;
		this.params = null;
		System.out.println("Init Provider");
	}
    
    
	public IdspServerSocket(int attestationType, int attestationMask, SSLContextParameters params) {
		// Create consumer socket
		this.attestationType = attestationType;
		this.attestationMask = attestationMask;
		this.params = params;
		System.out.println("Init Consumer");
	}

	/**
	 * Called upon incoming connection to server.
	 * 
	 * @param session
	 */
	@OnWebSocketConnect
    public void onOpen(Session session) {
        LOG.debug("Websocket opened " + this + " from " + session.getRemoteAddress().toString() + " to " + session.getLocalAddress().toString());
        IdsAttestationType type;
        switch(this.attestationType) {
	    	case 0:            
	    		type = IdsAttestationType.BASIC;
	    		break;
	    	case 1:
	    		type = IdsAttestationType.ALL;
	    		break;
	    	case 2:
	    		type = IdsAttestationType.ADVANCED;
	    		break;
	    	case 3:
	    		type = IdsAttestationType.ZERO;
	    		break;
	    	default:
	    		type = IdsAttestationType.BASIC;
	    		break;	    		
        }
        // create Finite State Machine for IDS protocol
        machine = new ProtocolMachine();
    	System.out.println("Starting provider");
    	fsm = machine.initIDSProviderProtocol(session, type, this.attestationMask);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        LOG.debug("websocket closed - reconnecting");
        fsm.reset();
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        LOG.debug("websocket on error", t);
        if (fsm!=null) {
        	fsm.reset();
        }
    }

    /**
     * Handles incoming messages to server.
     * 
     * @param session
     * @param frame
     */
    @OnWebSocketFrame
    public void onMessage(Session session, Frame frame) {
    	byte[] message = new byte[frame.getPayload().remaining()];
    	frame.getPayload().get(message);
    	LOG.debug("Received in state " + fsm.getState() + ": " + new String(message));
    	try {
    		lock.lockInterruptibly();
    		try {
        		if (fsm.getState().equals(ProtocolState.IDSCP_END.id()) || fsm.getState().equals(ProtocolState.IDSCP_ERROR.id())) {
        			System.out.println("Passing through to web socket " + new String(message));
        			return;
        		}
    			ConnectorMessage msg = ConnectorMessage.parseFrom(message);
    			fsm.feedEvent(new Event(msg.getType(), new String(message), msg));
    		} catch (InvalidProtocolBufferException e) {
    			LOG.error(e.getMessage(), e);
    			fsm.feedEvent(new Event(ConnectorMessage.Type.ERROR, e.getMessage(), ConnectorMessage.getDefaultInstance()));
    		}
    	} catch (InterruptedException e) {
			LOG.warn(e.getMessage());
		} finally {
			lock.unlock();
		}
    }
    
    public ReentrantLock semaphore() {
    	return lock;
    }

    public Condition isFinished() {
    	return isFinishedCond;
    }
	
    //get the result of the remote attestation
	public boolean isAttestationSuccessful() {
		return machine.getIDSCPConsumerSuccess();
	}

    //get the result of the remote attestation
	public AttestationResult getAttestationResult() {
		if (machine.getAttestationType()==IdsAttestationType.ZERO) {
			return AttestationResult.SKIPPED;
		} else {
			if (machine.getIDSCPConsumerSuccess()) {
				return AttestationResult.SUCCESS;
			} else {
				return AttestationResult.FAILED;
			}
		}
	}
}
