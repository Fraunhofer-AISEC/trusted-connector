package de.fhg.ids.comm.ws.protocol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;

import com.google.protobuf.MessageLite;
import com.ning.http.client.ws.WebSocket;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.RatType;
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
		fsm.addState("RAT:AWAIT_P_NONCE");
		fsm.addState("RAT:AWAIT_C_NONCE");
		fsm.addState("RAT:AWAIT_LEAVE");
		fsm.addState("SUCCESS");
		
		RemoteAttestationClientHandler h = new RemoteAttestationClientHandler(fsm);
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("start rat", "START", "RAT:AWAIT_CONFIRM", (e) -> {return replyProto(IdsProtocolMessages.EnterRatReq.newBuilder().setType(RatType.ENTER_RAT_REQUEST).build());} ));
		fsm.addTransition(new Transition(RatType.ENTER_RAT_RESPONSE, "RAT:AWAIT_CONFIRM", "RAT:AWAIT_P_NONCE", (e) -> {return replyProto(h.sendClientIdAndNonce(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_P_MY_NONCE, "RAT:AWAIT_P_NONCE", "RAT:AWAIT_C_NONCE", (e) -> {return replyProto(h.sendPcr(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_P_YOUR_NONCE, "RAT:AWAIT_C_NONCE", "RAT:AWAIT_LEAVE", (e) -> {return replyProto(h.sendSignedServerNonce(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_LEAVE, "RAT:AWAIT_LEAVE", "SUCCESS", (e) -> {return true;} ));
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {System.out.println(e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("START");
		
		return fsm;
	}
	
	public FSM initIDSProviderProtocol(Session sess) {
		this.sess = sess;
		FSM fsm = new FSM();
		fsm.addState("AWAIT_SELECT_CONV");
		fsm.addState("RAT:AWAIT_C_NONCE");
		fsm.addState("RAT:AWAIT_PCR");
		fsm.addState("RAT:AWAIT_P_NONCE");
		fsm.addState("SUCCESS");
		
		RemoteAttestationServerHandler h = new RemoteAttestationServerHandler(fsm);

		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition(RatType.ENTER_RAT_REQUEST, "AWAIT_SELECT_CONV", "RAT:AWAIT_C_NONCE", (e) -> {return replyProto(h.replyToRatRequest(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_C_MY_NONCE, "RAT:AWAIT_C_NONCE", "RAT:AWAIT_PCR", (e) -> {return replyProto(h.sendServerNonceAndCert(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_C_REQ_PCR, "RAT:AWAIT_PCR", "RAT:AWAIT_P_NONCE", (e) -> {return replyProto(h.sendSignedClientNonce(e));} ));
		fsm.addTransition(new Transition(RatType.RAT_C_YOUR_NONCE, "RAT:AWAIT_P_NONCE", "SUCCESS", (e) -> {return replyProto(h.leaveRat(e));} ));
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {System.out.println(e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("AWAIT_SELECT_CONV");
		
		return fsm;
	}


	boolean replyProto(MessageLite message) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
			System.out.println("Sending out " + text.length + " bytes");
			ws.sendMessage(text);
		} else if (sess!=null) {
			try {
				ByteBuffer bb = ByteBuffer.wrap(text);
				System.out.println("Sending out " + bb.array().length + " bytes");
				sess.getRemote().sendBytes(bb);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}
