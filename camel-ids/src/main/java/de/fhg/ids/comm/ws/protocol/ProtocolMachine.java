package de.fhg.ids.comm.ws.protocol;


import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;

import com.ning.http.client.ws.WebSocket;

import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fhg.ids.comm.ws.protocol.fsm.Transition;

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
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("start rat", "START", "RAT:AWAIT_CONFIRM", (e) -> {return reply("enter rat");} ));
		fsm.addTransition(new Transition("entering rat", "RAT:AWAIT_CONFIRM", "RAT:AWAIT_P_NONCE", (e) -> {return reply("c_my_nonce");} ));
		fsm.addTransition(new Transition("p_my_nonce", "RAT:AWAIT_P_NONCE", "RAT:AWAIT_C_NONCE", (e) -> {return reply("c_pcr");} ));
		fsm.addTransition(new Transition("p_your_nonce", "RAT:AWAIT_C_NONCE", "RAT:AWAIT_LEAVE", (e) -> {return reply("c_your_nonce");} ));
		fsm.addTransition(new Transition("leaving rat", "RAT:AWAIT_LEAVE", "SUCCESS", (e) -> {return true;} ));
		
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
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("enter rat", "AWAIT_SELECT_CONV", "RAT:AWAIT_C_NONCE", (e) -> {return reply("entering rat");} ));
		fsm.addTransition(new Transition("c_my_nonce", "RAT:AWAIT_C_NONCE", "RAT:AWAIT_PCR", (e) -> {return reply("p_my_nonce");} ));
		fsm.addTransition(new Transition("c_pcr", "RAT:AWAIT_PCR", "RAT:AWAIT_P_NONCE", (e) -> {return reply("p_your_nonce");} ));
		fsm.addTransition(new Transition("c_your_nonce", "RAT:AWAIT_P_NONCE", "SUCCESS", (e) -> {return reply("leaving rat");} ));
		
		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {System.out.println(e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("AWAIT_SELECT_CONV");
		
		return fsm;
	}

	/** 
	 * Sends a response over the websocket session.
	 * 
	 * @param text
	 * @return true if successful, false if not.
	 */
	private boolean reply(String text) {
		if (ws!=null) {
			ws.sendMessage(text);
		} else if (sess!=null) {
			try {
				sess.getRemote().sendString(text);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}
