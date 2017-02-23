package de.fhg.ids.comm.ws.protocol.fsm;

import de.fhg.ids.comm.ws.protocol.ProtocolState;

/**
 * A Transition transfers the FSM from a start state to an end state and is
 * triggered by an event.
 * 
 * Associated with a transition is code that is executed when the transition is
 * taken.
 * 
 */
public class Transition {
	Object evtName;
	String startState;
	String endState;
	TransitionListener before;

	/**
	 * Creates a new transition.
	 * 
	 * @param evtName
	 *            the event triggering this transition.
	 * @param startState
	 *            the start state of this transition.
	 * @param endState
	 *            the end state of this transition.
	 * @param runBeforeTransition
	 *            code executed when the transition is triggered. If the
	 *            callable returns true, the transition takes place, if the
	 *            callable returns false, the transition will not take place and
	 *            the FSM remains in startState.
	 */
	public Transition(String evtName, String startState, String endState, TransitionListener runBeforeTransition) {
		this.evtName = evtName;
		this.startState = startState;
		this.endState = endState;
		this.before = runBeforeTransition;
	}

	public Transition(Object evtKey, String startState, String endState, TransitionListener runBeforeTransition) {
		this.evtName = evtKey;
		this.startState = startState;
		this.endState = endState;
		this.before = runBeforeTransition;
	}

	public Transition(Object evtKey, ProtocolState startState, ProtocolState endState, TransitionListener runBeforeTransition) {
		this.evtName = evtKey;
		this.startState = startState.id();
		this.endState = endState.id();
		this.before = runBeforeTransition;
	}

	/**
	 * Executes code associated with this transition.
	 * 
	 * This method returns the result of the executed Callable (true or false)
	 * or false in case of any Throwables. No exceptions will be thrown from
	 * this method.
	 * 
	 * @return
	 */
	protected boolean doBeforeTransition(Event event) {
		boolean success = true;
		if (before != null) {
			try {
				success = before.transition(event);
			} catch (Throwable t) {
				success = false;
				t.printStackTrace();
			}
		}
		return success;
	}
}