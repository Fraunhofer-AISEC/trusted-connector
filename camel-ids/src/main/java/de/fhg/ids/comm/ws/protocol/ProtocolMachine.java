package de.fhg.ids.comm.ws.protocol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.ids.comm.ws.protocol.error.ErrorHandler;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataConsumerHandler;
import de.fhg.ids.comm.ws.protocol.metadata.MetadataHandler;
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
	/** The session to send and receive messages */
	private WebSocket ws;
	private Session sess;
	private String ttpURL = "http://10.1.2.19:31337/configurations/check";
	private String socket = "/var/run/tpm2d/control.sock";
	private Logger LOG = LoggerFactory.getLogger(ProtocolMachine.class);

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
	public FSM initIDSConsumerProtocol(WebSocket websocket, IdsAttestationType type) {
		this.ws = websocket;
		FSM fsm = new FSM();
		URI ttp = null;
		try {
			ttp = new URI(ttpURL);
		} catch (URISyntaxException e1) {
			LOG.debug("TTP URI Syntax exception");
			e1.printStackTrace();
		}
		// all handler
		RemoteAttestationConsumerHandler ratConsumerHandler = new RemoteAttestationConsumerHandler(fsm, type, ttp, socket);
		ErrorHandler errorHandler = new ErrorHandler();
		MetadataConsumerHandler metaHandler = new MetadataConsumerHandler();		
		
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
				
				//Metadata exchange
				fsm.addState(ProtocolState.IDSCP_META_REQUEST);
				fsm.addState(ProtocolState.IDSCP_META_RESPONSE);
				
				/* Remote Attestation Protocol */
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, (e) -> {return replyProto(ratConsumerHandler.enterRatRequest(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, (e) -> {return replyProto(ratConsumerHandler.sendTPM2Ddata(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_RAT_AWAIT_RESULT, (e) -> {return replyProto(ratConsumerHandler.sendResult(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, (e) -> {return replyProto(ratConsumerHandler.leaveRatRequest(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {return ratConsumerHandler.isSuccessful();} ));
				
				/* Metadata Exchange Protocol */ 
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_META_REQUEST, (e) -> {return replyProto(metaHandler.request(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_REQUEST, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_META_RESPONSE, (e) -> {return replyProto(metaHandler.response(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_RESPONSE, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_META_LEAVE, (e) -> {return replyProto(metaHandler.leave(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_LEAVE, ProtocolState.IDSCP_META_LEAVE, ProtocolState.IDSCP_START, (e) -> {return true;} ));
				
				/* error protocol */
				// in case of error go back to IDSC_START state
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESULT, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_REQUEST, true);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_RESPONSE, true);} ));
				break;

			case ZERO:
				/* NO Remote Attestation Protocol at all */
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_END, (e) -> {return replyProto(ratConsumerHandler.sendNoAttestation(e));} ));			
				break;
			
			default:
				/* attestation type is missing so do error routine */
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, true);} ));
				break;			
		}

		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Consumer State change: " + e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState(ProtocolState.IDSCP_START);
		
		return fsm;
	}
	
	public FSM initIDSProviderProtocol(Session sess, IdsAttestationType type) {
		this.sess = sess;
		FSM fsm = new FSM();
		URI ttp = null;
		try {
			ttp = new URI(ttpURL);
		} catch (URISyntaxException e1) {
			LOG.debug("TTP URI Syntax exception");
			e1.printStackTrace();
		}

		// all handler
		RemoteAttestationProviderHandler ratProviderHandler = new RemoteAttestationProviderHandler(fsm, type, ttp, socket);
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
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_END, (e) -> {return replyProto(ratProviderHandler.leaveRatRequest(e));} ));
				
				/* Metadata Exchange Protocol */ 
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_REQUEST, ProtocolState.IDSCP_START, ProtocolState.IDSCP_META_REQUEST, (e) -> {return replyProto(metaHandler.request(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_RESPONSE, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_META_RESPONSE, (e) -> {return replyProto(metaHandler.response(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_RESPONSE, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_META_LEAVE, (e) -> {return replyProto(metaHandler.leave(e));} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.META_LEAVE, ProtocolState.IDSCP_META_LEAVE, ProtocolState.IDSCP_START, (e) -> {return true;} ));
				
				/* error protocol */
				// in case of error go back to IDSC_START state
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_START, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_REQUEST, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESPONSE, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_RESULT, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_RESULT, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_RAT_AWAIT_LEAVE, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_REQUEST, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_REQUEST, false);} ));
				fsm.addTransition(new Transition(ConnectorMessage.Type.ERROR, ProtocolState.IDSCP_META_RESPONSE, ProtocolState.IDSCP_START, (e) -> {return errorHandler.handleError(e, ProtocolState.IDSCP_META_RESPONSE, false);} ));
				break;

			case ZERO:
				/* NO Remote Attestation Protocol at all */
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_END, (e) -> {return true;} ));			
				break;
			
			default:
				/* attestation type is missing so do error routine */
				fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_START, ProtocolState.IDSCP_START, ProtocolState.IDSCP_START, (e) -> { return errorHandler.handleError(e, ProtocolState.IDSCP_START, false);} ));
				break;
		}
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Provider State change: " + e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState(ProtocolState.IDSCP_START);
		
		return fsm;
	}


	boolean replyProto(MessageLite message) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		//System.out.println("message to send: \n" + message.toString() + "\n");
		try {
			message.writeTo(bos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return reply(bos.toByteArray());
	}

	/** 
	 * Sends a response over the websocket session.
	 * 
	 * @param text
	 * @return true if successful, false if not.
	 */
	boolean reply(byte[] text) {
		if (ws!=null) {
			//System.out.println("Sending out " + text.length + " bytes");
			ws.sendMessage(text);
		} else if (sess!=null) {
			try {
				ByteBuffer bb = ByteBuffer.wrap(text);
				//System.out.println("Sending out ByteBuffer with " + bb.array().length + " bytes");
				sess.getRemote().sendBytes(bb);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}
