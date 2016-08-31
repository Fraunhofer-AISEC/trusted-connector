package de.fhg.ids.comm.ws.protocol;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;

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
	private Session s;

	/** Do not call this one */
	@SuppressWarnings("unused")
	private ProtocolMachine() { }
	
	/** Call this one */
	public ProtocolMachine(Session session) {
		if (session == null) {
			throw new NullPointerException("Null session not allowed");
		}
		this.s = session;
	}

	/**
	 * Returns a finite state machine (FSM) implementing the IDSP protocol.
	 * 
	 * The FSM will be in its initial state and ready to accept messages via <code>FSM.feedEvent()</code>.
	 * It will send responses over the session according to its FSM definition.
	 * 
	 * @return a FSM implementing the IDSP protocol.
	 */
	public FSM initIDSProtocol() {
		FSM fsm = new FSM();
		fsm.addState("SELECT_CONV");
		fsm.addState("RAT:AWAIT_NONCE");
		fsm.addState("RAT:AWAIT_ATT_REQUEST");
		fsm.addState("MEX");		
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("enter rat", "SELECT_CONV", "RAT:AWAIT_NONCE", (e) -> {return reply("entering rat");} ));
		fsm.addTransition(new Transition("nonce", "RAT:AWAIT_NONCE", "RAT:AWAIT_ATT_REQUEST", (e) -> {return reply("server hello");} ));
		fsm.addTransition(new Transition("request attestation", "RAT:AWAIT_ATT_REQUEST", "SELECT_CONV", (e) -> {return reply("attestation");} ));
		
		/* Meta Data Exchange Protocol */
		fsm.addTransition(new Transition("enter mex", "SELECT_CONV", "MEX", (e) -> {return reply("entering mex");} ));

		/* Add listener to log state transitions*/
		fsm.addSuccessfulChangeListener((f,e) -> {System.out.println(e.getKey() + " -> " + f.getState());});
		
		/* Run the FSM */
		fsm.setInitialState("SELECT_CONV");
		
		return fsm;
	}
	
	/** 
	 * Sends a response over the websocket session.
	 * 
	 * @param text
	 * @return true if successful, false if not.
	 */
	private boolean reply(String text) {
		try {
			s.getRemote().sendString(text);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
