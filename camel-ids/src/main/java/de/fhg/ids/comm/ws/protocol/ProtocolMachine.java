package de.fhg.ids.comm.ws.protocol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.asynchttpclient.ws.WebSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.IdsAttestationType;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationClientHandler;
import de.fhg.ids.comm.ws.protocol.rat.RemoteAttestationServerHandler;

/**
 * Generator of protocols over a websocket session.
 * 
 * @author Julian Schütte
 *
 */
public class ProtocolMachine {
	/** The session to send and receive messages */
	private WebSocket ws;
	private Session sess;
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
	public FSM initIDSConsumerProtocol(WebSocket websocket) {
		this.ws = websocket;
		FSM fsm = new FSM();
		fsm.addState("START");
		fsm.addState("RAT:AWAIT_CONFIRM");
		fsm.addState("RAT:AWAIT_RESULT");
		fsm.addState("RAT:AWAIT_LEAVE");
		fsm.addState("SUCCESS");
		fsm.addState("MEX:AWAIT_CONFIRM");
		fsm.addState("MEX:AWAIT_LEAVE");
		
		
		RemoteAttestationClientHandler h = new RemoteAttestationClientHandler(fsm, IdsAttestationType.BASIC);
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("start rat", "START", "RAT:AWAIT_CONFIRM", (e) -> {return replyProto(h.enterRatRequest(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESPONSE, "RAT:AWAIT_CONFIRM", "RAT:AWAIT_RESULT", (e) -> {return replyProto(h.sendTPM2Ddata(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, "RAT:AWAIT_RESULT", "RAT:AWAIT_LEAVE", (e) -> {return replyProto(h.sendResult(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, "RAT:AWAIT_LEAVE", "SUCCESS", (e) -> {return replyProto(h.leaveRatRequest(e));} ));
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Consumer State change: " + e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("START");
		
		return fsm;
	}
	
	public FSM initIDSProviderProtocol(Session sess) {
		this.sess = sess;
		FSM fsm = new FSM();
		fsm.addState("AWAIT_RAT");
		fsm.addState("RAT:AWAIT_CONFIRM");
		fsm.addState("RAT:AWAIT_RESULT");
		fsm.addState("RAT:AWAIT_LEAVE");
		fsm.addState("SUCCESS");
		fsm.addState("ERROR");
		
		RemoteAttestationServerHandler h = new RemoteAttestationServerHandler(fsm, IdsAttestationType.BASIC);

		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_REQUEST, "AWAIT_RAT", "RAT:AWAIT_CONFIRM", (e) -> {return replyProto(h.sendTPM2Ddata(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESPONSE, "RAT:AWAIT_CONFIRM", "RAT:AWAIT_RESULT", (e) -> {return replyProto(h.sendResult(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_RESULT, "RAT:AWAIT_RESULT", "RAT:AWAIT_LEAVE", (e) -> {return replyProto(h.leaveRatRequest(e));} ));
		fsm.addTransition(new Transition(ConnectorMessage.Type.RAT_LEAVE, "RAT:AWAIT_LEAVE", "SUCCESS", (e) -> {return true;} ));
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {LOG.debug("Provider State change: " + e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("START");
		
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
