package de.fhg.ids.comm.ws.protocol.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

import de.fhg.ids.comm.ws.protocol.ProtocolState;

/**
 * Implementation of a finite state machine (FSM).
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class FSM {

	protected String currentState = null;
	protected Map<String, State> states;
	protected HashSet<ChangeListener> successFullChangeListeners;
	protected HashSet<ChangeListener> failedChangeListeners;
	private String initialState = null;

	public FSM() {
		this.states = new HashMap<>();
		this.successFullChangeListeners = new HashSet<>();
		this.failedChangeListeners = new HashSet<>();
	}

	public String getState() {
		return currentState;
	}

	public void addState(String state) {
		addState(state, null, null, null);
	}

	public void addState(ProtocolState state) {
		addState(state.id(), null, null, null);
	}
	
	/**
	 * Defines an additional state. A state is identified by a name and has
	 * three blocks of code assigned to it:
	 * 
	 * 1) entryCode is executed when the state is entered from another state
	 * (after the transition's code has been executed)
	 * 
	 * 2) exitCode is executed when the state is left for another state (before
	 * the transition's code is executed)
	 * 
	 * 3) alwaysRunCode is always executed when the FSM enters this state (even
	 * if it has been in that state before)
	 * 
	 * All three Runnables may be null. Their result has no impact on the FSM's
	 * state, exceptions thrown are ignored.
	 * 
	 * @param state
	 * @param entryCode
	 * @param exitCode
	 * @param alwaysRunCode
	 */
	public void addState(String state, Runnable entryCode, Runnable exitCode, Runnable alwaysRunCode) {
		if (states.size() == 0) {
			this.initialState = state;
			this.currentState = state;
		}
		if (!states.containsKey(state)) {
			states.put(state, new State(entryCode, exitCode, alwaysRunCode));
		} else {
			throw new IllegalArgumentException("State already exists: " + state);
		}
	}

	/**
	 * Defines initial state of this FSM. If no initial state is defined
	 * explicitly, the first added state is the initial state.
	 * 
	 * @param state
	 */
	public void setInitialState(String state) {
		this.initialState = state;
	}
	
	public void setInitialState(ProtocolState state) {
		this.initialState = state.id();
	}

	/**
	 * Resets FSM to it initial state
	 */
	public void reset() {
		this.currentState = this.initialState;
	}

	private void setState(String state) {
		boolean runExtraCode = !state.equals(currentState);
		if (runExtraCode && currentState != null) {
			states.get(currentState).runExitCode();
		}
		currentState = state;
		states.get(currentState).runAlwaysCode();
		if (runExtraCode) {
			states.get(currentState).runEntryCode();
		}

		/* If event-less transition is defined for current node, trigger it
		   immediately */
		if (states.get(currentState).transitions.containsKey(null)) {
			feedEvent(null);
		}
	}

	/**
	 * Add a new transition to this FSM. 
	 * 
	 * @param trans
	 */
	public void addTransition(Transition trans) {
		State st = states.get(trans.startState);
		if (st == null || !states.containsKey(trans.endState)) {
			throw new NoSuchElementException("Missing state: " + trans.startState);
		}
		st.addTransition(trans);
	}

	public void addSuccessfulChangeListener(ChangeListener cl) {
		successFullChangeListeners.add(cl);
	}

	public void addFailedChangeListener(ChangeListener cl) {
		failedChangeListeners.add(cl);
	}

	public void feedEvent(Event event) {
		Object evtKey = event.getKey();
		State state = states.get(currentState);
		if (state.transitions.containsKey(evtKey)) {
			Transition trans = state.transitions.get(evtKey);

			if (trans.doBeforeTransition(event)) {
				setState(trans.endState);
				successFullChangeListeners.forEach(l -> l.stateChanged(this, event));
			} else {
				failedChangeListeners.forEach(l -> l.stateChanged(this, event));
			}
		}
	}
	
	public String toDot() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph finite_state_machine {\n");
		sb.append("	rankdir=LR;\n");
		sb.append("	node [shape = ellipse];\n");
		for (String from : states.keySet()) {
			for (Object t : states.get(from).transitions.keySet()) {
				String to = states.get(from).transitions.get(t).endState;
				Object eventKey = states.get(from).transitions.get(t).evtName;
				sb.append("    " + from.replace(':', '_') + " -> " + to.replace(':', '_') + " [ label=\""+eventKey+"\" ];\n");
			}
		}
		sb.append("			}");
		return sb.toString();
	}
}