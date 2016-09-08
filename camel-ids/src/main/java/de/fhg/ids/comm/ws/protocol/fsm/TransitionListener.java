package de.fhg.ids.comm.ws.protocol.fsm;

public interface TransitionListener {
	boolean transition(Event event);
}
