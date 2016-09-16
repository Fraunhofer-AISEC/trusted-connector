package de.fhg.ids.comm.ws.protocol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;

import com.google.protobuf.MessageLite;
import com.ning.http.client.ws.WebSocket;

import de.fhg.aisec.ids.messages.IdsProtocolMessages;
import de.fhg.aisec.ids.messages.IdsProtocolMessages.MessageType;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;
import de.fhg.ids.comm.ws.protocol.rat.MetaExchangeClientHandler;
import de.fhg.ids.comm.ws.protocol.rat.MetaExchangeServerHandler;
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
		fsm.addState("MEX:AWAIT_CONFIRM");
		fsm.addState("MEX:AWAIT_LEAVE");
		
		RemoteAttestationClientHandler h = new RemoteAttestationClientHandler(fsm);
		MetaExchangeClientHandler hmex = new MetaExchangeClientHandler(fsm);
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("start rat", "START", "RAT:AWAIT_CONFIRM", (e) -> {return replyProto(IdsProtocolMessages.EnterRatReq.newBuilder().setType(MessageType.ENTER_RAT_REQUEST).build());} ));
		fsm.addTransition(new Transition(MessageType.ENTER_RAT_RESPONSE, "RAT:AWAIT_CONFIRM", "RAT:AWAIT_P_NONCE", (e) -> {return replyProto(h.sendClientIdAndNonce(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_P_MY_NONCE, "RAT:AWAIT_P_NONCE", "RAT:AWAIT_C_NONCE", (e) -> {return replyProto(h.sendPcr(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_P_YOUR_NONCE, "RAT:AWAIT_C_NONCE", "RAT:AWAIT_LEAVE", (e) -> {return replyProto(h.sendSignedServerNonce(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_LEAVE, "RAT:AWAIT_LEAVE", "MEX:AWAIT_CONFIRM", (e) -> {return replyProto(hmex.enterMex(e));} ));
		
		/* Meta Data Exchange Protocol (starts immediately after rat) */
		//fsm.addTransition(new Transition(MessageType.ENTER_MEX_REQUEST, "RAT:SUCCESS", "MEX:AWAIT_CONFIRM", (e) -> {return replyProto(hmex.enterMex(e));}));
		fsm.addTransition(new Transition(MessageType.ENTER_MEX_RESPONSE, "MEX:AWAIT_CONFIRM", "MEX:AWAIT_LEAVE", (e) -> {return replyProto(hmex.sendClientValues(e));}));
		fsm.addTransition(new Transition(MessageType.MEX_LEAVE, "MEX:AWAIT_LEAVE", "SUCCESS", (e) -> {return true;}));
		
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
		fsm.addState("MEX:AWAIT_VALUES");
		fsm.addState("SUCCESS");
		
		RemoteAttestationServerHandler h = new RemoteAttestationServerHandler(fsm);
		MetaExchangeServerHandler hmex = new MetaExchangeServerHandler(fsm);

		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition(MessageType.ENTER_RAT_REQUEST, "AWAIT_SELECT_CONV", "RAT:AWAIT_C_NONCE", (e) -> {return replyProto(h.replyToRatRequest(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_C_MY_NONCE, "RAT:AWAIT_C_NONCE", "RAT:AWAIT_PCR", (e) -> {return replyProto(h.sendServerNonceAndCert(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_C_REQ_PCR, "RAT:AWAIT_PCR", "RAT:AWAIT_P_NONCE", (e) -> {return replyProto(h.sendSignedClientNonce(e));} ));
		fsm.addTransition(new Transition(MessageType.RAT_C_YOUR_NONCE, "RAT:AWAIT_P_NONCE", "AWAIT_SELECT_CONV", (e) -> {return replyProto(h.leaveRat(e));} ));
		
		/* Meta Data Exchange Protocol */
		fsm.addTransition(new Transition(MessageType.ENTER_MEX_REQUEST, "AWAIT_SELECT_CONV", "MEX:AWAIT_VALUES", (e) -> {return replyProto(hmex.sendEnterMexResponse(e));}));
		fsm.addTransition(new Transition(MessageType.MEX_C_MY_VALUES, "MEX:AWAIT_VALUES", "SUCCESS", (e) -> {return replyProto(hmex.sendLeave(e));}));
		
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
