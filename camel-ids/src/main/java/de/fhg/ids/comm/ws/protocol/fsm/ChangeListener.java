package de.fhg.ids.comm.ws.protocol.fsm;

/**
 * Functional interface for listeners which are notified whenever the state of a
 * FSM changes.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface ChangeListener {

	void stateChanged(FSM fsm, Event event);

}
