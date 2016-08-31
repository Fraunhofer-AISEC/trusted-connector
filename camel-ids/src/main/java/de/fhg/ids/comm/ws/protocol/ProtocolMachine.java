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
		fsm.addState("AWAIT_CONFIRM");
		fsm.addState("SUCCESS");
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("start rat", "START", "AWAIT_CONFIRM", (e) -> {return reply("enter rat protocol");} ));
		fsm.addTransition(new Transition("entering rat", "AWAIT_CONFIRM", "SUCCESS", (e) -> {return true;} ));
		
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
		fsm.addState("SUCCESS");
		
		/* Remote Attestation Protocol */
		fsm.addTransition(new Transition("enter rat protocol", "AWAIT_SELECT_CONV", "SUCCESS", (e) -> {return reply("entering rat");} ));
		
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
