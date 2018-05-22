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
package de.fhg.ids.comm.ws.protocol;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.ids.comm.ws.protocol.error.ErrorHandler;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataProviderHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationConsumerHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationProviderHandler;

/**
 * Generator of protocols over a websocket session.
 * 
 * @author Julian Schütte
 * @author Georg Räß
 * @author Gerd Brost
 *
 */
public class ProtocolMachine {
	private static final String TTP_URI_PROTOCOL = "https";
	private static final String TTP_URI_ENDPOINT_CHECK = "/configurations/check";
	private static final Logger LOG = LoggerFactory.getLogger(ProtocolMachine.class);
	
	/** The session to send and receive messages */
	private Session serverSession;
	private boolean ratConsumerSuccess = false;
	private boolean ratProviderSuccess = false;
	private String socket = "/var/run/tpm2d/control.sock";
	private IdsAttestationType attestationType;
	private WebSocket clientSocket;
	
	/** C'tor */
	public ProtocolMachine() { }
	
	/**
	 * Returns a finite state machine (FSM) implementing the IDSP protocol.
	 * 
	 * The FSM will be in its initial state and ready to accept messages via <code>FSM.feedEvent()</code>.
	 * It will send responses over the session according to its FSM definition.
	 * 
	 * @return a FSM implementing the IDSP protocol.
	 */
	public FSM initIDSConsumerProtocol(WebSocket ws, IdsAttestationType attestationType, int attestationMask, SSLContextParameters params) {
		this.clientSocket = ws;
		this.attestationType = attestationType;
		FSM fsm = new FSM();
		try {
			// set trusted third party URL
			URI ttp = getTrustedThirdPartyURL();
			// all handler
			RemoteAttestationConsumerHandler ratConsumerHandler = new RemoteAttestationConsumerHandler(fsm, attestationType, attestationMask, ttp, socket);
			ErrorHandler errorHandler = new ErrorHandler();
			MetadataConsumerHandler metaHandler = new MetadataConsumerHandler();		
			
			// standard protocol states
			fsm.addState(ProtocolState.IDSCP_START);
			fsm.addState(ProtocolState.IDSCP_ERROR);
			fsm.addState(ProtocolState.IDSCP_END);
			
			switch(attestationType) {
				case BASIC:
				case ALL:
				case ADVANCED:
					// do remote attestation 
					// rat states
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);
					
					//Metadata exchange
					fsm.addState(ProtocolState.IDSCP_META_REQUEST);
					fsm.addState(ProtocolState.IDSCP_META_RESPONSE);
					
					/* Remote Attestation Protocol */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, (e) -> {return replyProto(ratConsumerHandler.enterRatRequest(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, (e) -> {return replyProto(ratConsumerHandler.sendTPM2Ddata(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESULT, (e) -> {return replyProto(ratConsumerHandler.sendResult(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, (e) -> {return replyProto(ratConsumerHandler.leaveRatRequest(e));} ));
					//fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {return setIDSCPConsumerSuccess(ratConsumerHandler.isSuccessful());} ));
					
					/* Metadata Exchange Protocol */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_META_REQUEST, (e) -> {return replyProto(metaHandler.request(e));} ));
					//fsm.addTransition(new Transition(ConnectorMessage.Type.META_REQUEST, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_META_RESPONSE, (e) -> {return replyProto(metaHandler.response(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.META_RESPONSE, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_END, (e) -> {return true;} ));
					
					/* error protocol */
					// in case of error go back to IDSC_START state
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_START, ProtocolState.IDSCP_END, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESULT, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_REQUEST, true);} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_END, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_RESPONSE, true);} ));
					break;

				case ZERO:
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
					
					/* NO Remote Attestation Protocol at all */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_RAT_AWAIT_RESULT, (e) -> {return replyProto(ratConsumerHandler.sendNoAttestation(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_END, (e) -> {return setIDSCPConsumerSuccess(true);} ));
					break;
				
				default:
					/* attestation type is missing so do error routine */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, true);} ));
					break;			
			}
			
			/* Add listener to log state transitions*/
			fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Consumer State change: " + e.getKey() + " -> " + f.getState());});
//			String graph = fsm.toDot();
//			System.out.println(graph);
		} catch (URISyntaxException e) {
			LOG.error("TTP URI Syntax exception", e);
		}
		
		/* Run the FSM */
		fsm.setInitialState(ProtocolState.IDSCP_START);
		
		return fsm;
	}
	
	public FSM initIDSProviderProtocol(Session sess, IdsAttestationType type, int attestationMask, File tpmdSocket) {
		this.attestationType = type;
		this.serverSession = sess;
		FSM fsm = new FSM();
		try {
			// set trusted third party URL
			URI ttp = getTrustedThirdPartyURL();

			// all handler
			RemoteAttestationProviderHandler ratProviderHandler = new RemoteAttestationProviderHandler(fsm, type, attestationMask, ttp, socket);
			ErrorHandler errorHandler = new ErrorHandler();
			MetadataProviderHandler metaHandler = new MetadataProviderHandler();
			
			// standard protocol states
			fsm.addState(ProtocolState.IDSCP_START);
			fsm.addState(ProtocolState.IDSCP_ERROR);
			fsm.addState(ProtocolState.IDSCP_END);
			
			switch(type) {
				case BASIC:
				case ALL:
				case ADVANCED:
					// do remote attestation
					// rat states
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_REQUEST);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESPONSE);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_RESULT);
					fsm.addState(ProtocolState.IDSCP_RAT_AWAIT_LEAVE);
					
					//metadata exchange states
					fsm.addState(ProtocolState.IDSCP_META_REQUEST);
					fsm.addState(ProtocolState.IDSCP_META_RESPONSE);
					
					/* Remote Attestation Protocol */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, ProtocolState.IDSCP_START, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, (e) -> {return replyProto(ratProviderHandler.enterRatRequest(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESULT, (e) -> {return replyProto(ratProviderHandler.sendTPM2Ddata(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, (e) -> {return replyProto(ratProviderHandler.sendResult(e));} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {return replyProto(ratProviderHandler.leaveRatRequest(e)) && setIDSCPProviderSuccess(ratProviderHandler.isSuccessful());} ));
					
					/* Metadata Exchange Protocol */ 
					fsm.addTransition(new Transition(ConnectorMessage.Type.META_REQUEST, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_END, (e) -> {return replyProto(metaHandler.response(e));} ));
					//fsm.addTransition(new Transition(ConnectorMessage.Type.META_RESPONSE, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_END, (e) -> {return true;} ));
					
					/* error protocol */
					// in case of error go back to IDSC_START state
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_START, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_START, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESULT, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_META_REQUEST, false); return replyAbort();} ));
					fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_END, (e) -> {errorHandler.handleError(e, ProtocolState.IDSCP_META_RESPONSE, false); return replyAbort();} ));
					break;

				case ZERO:
					/* NO Remote Attestation Protocol at all */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, ProtocolState.IDSCP_START, ProtocolState.IDSCP_END, (e) -> {return replyProto(ratProviderHandler.sendNoAttestation(e)) && setIDSCPProviderSuccess(true);} ));
					break;
				
				default:
					/* attestation type is missing so do error routine */
					fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, false);} ));
					break;
			}
			
			/* Add listener to log state transitions */
			fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Provider State change: " + e.getKey() + " -> " + f.getState());});
			String graph = fsm.toDot();
			System.out.println(graph);

		} catch (URISyntaxException e) {
			LOG.error("TTP URI Syntax exception", e);
		}

		/* Run the FSM */
		fsm.setInitialState(ProtocolState.IDSCP_START);
		
		return fsm;
	}

	private boolean setIDSCPProviderSuccess(boolean success) {
		this.ratProviderSuccess = success;
		return true;
	}

	private boolean setIDSCPConsumerSuccess(boolean success) {
		this.ratConsumerSuccess = success;
		return true;
	}

	public IdsAttestationType getAttestationType() {
		return attestationType; 
	}
	public boolean getIDSCPProviderSuccess() {
		return this.ratProviderSuccess;
	}

	public boolean getIDSCPConsumerSuccess() {
		return this.ratConsumerSuccess;
	}

	private boolean replyProto(MessageLite message) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			message.writeTo(bos);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return reply(bos.toByteArray());
	}

	/** 
	 * Sends a response over the websocket session.
	 * 
	 * @param text
	 * @return true if successful, false if not.
	 */
	private boolean reply(byte[] text) {
		if (this.serverSession != null) {
			try {
				ByteBuffer bb = ByteBuffer.wrap(text);
				LOG.trace("Sending out ByteBuffer with " + bb.array().length + " bytes");
				serverSession.getRemote().sendBytes(bb);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		} else if (this.clientSocket != null) {
			this.clientSocket.sendMessage(text);
		}
		return true;
	}
	

	private boolean replyAbort() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		MessageLite abortMessage =  ConnectorMessage
				.newBuilder()
				.setId(0)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage("Abort")
						.build())
				.build();
		try {
			abortMessage.writeTo(bos);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return reply(bos.toByteArray());
	}
	
	/**
	 * Return URI of trusted third party (ttp).
	 * 
	 * The URI is constructed from host and port settings in the OSGi preferences service.
	 * 
	 * @return
	 * @throws URISyntaxException
	 */
	private URI getTrustedThirdPartyURL() throws URISyntaxException {
		// TODO Get it from from somewhere
		return null;
		//		Optional<PreferencesService> prefs = IdsProtocolComponent.getPreferencesService();
//		String ttpHost = "127.0.0.1";
//		String ttpPort = "31337";
//		if (prefs.isPresent() &&  prefs.get().getUserPreferences(Constants.PREFERENCES_ID)!=null) {
//			ttpHost = prefs.get().getUserPreferences(Constants.PREFERENCES_ID).get("ttp.host", "127.0.0.1");
//			ttpPort = prefs.get().getUserPreferences(Constants.PREFERENCES_ID).get("ttp.port", "31337");
//		}
//		return new URI(TTP_URI_PROTOCOL + "://" + ttpHost + ":" + ttpPort + TTP_URI_ENDPOINT_CHECK);
	}
}
