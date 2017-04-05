package de.fhg.ids.comm.ws.protocol.fsm;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a state with some number of associated transitions.
 */
class State {
	// map from event key to transition
	Map<Object, Transition> transitions;
	Runnable entryCode;
	Runnable exitCode;
	Runnable alwaysRunCode;

	State(Runnable entryCode, Runnable exitCode, Runnable alwaysRunCode) {
		transitions = new HashMap<Object, Transition>();
		this.entryCode = entryCode;
		this.exitCode = exitCode;
		this.alwaysRunCode = alwaysRunCode;
	}

	public void addTransition(Transition trans) {
		// Fail fast for duplicate transitions
		if (transitions.containsKey(trans.evtName)) {
			throw new IllegalArgumentException("Transition for event " + trans.evtName + " already exists.");
		}
		transitions.put(trans.evtName, trans);
	}

	public void runEntryCode() {
		if (entryCode != null) {
			entryCode.run();
		}
	}

	public void runExitCode() {
		if (exitCode != null) {
			exitCode.run();
		}
	}

	public void runAlwaysCode() {
		if (alwaysRunCode != null) {
			alwaysRunCode.run();
		}
	}
}