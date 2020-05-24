package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A State class for the Finite State Machine.
 * A state holds all outgoing transitions and a noTransitionHandler for events, that do not trigger
 * any outgoing available transition. Transitions are mapped with the event key.
 *
 * @author Leon Beckmann leon.beckmann@aisec.fraunhofer.de
 */
public class State {

    private final ConcurrentHashMap<Object, Transition> transitions = new ConcurrentHashMap<>();
    private Function<Event, State> noTransitionHandler = null;

    /*
     * A method for triggering aa transition of the current state by a given event
     *
     * If no transition exists for the given event, the noTransitionHandler is triggered
     *
     * Returns the target state of the triggered transition (new current state of the fsm)
     */
    State feedEvent(Event e) {
        Transition t = transitions.get(e.getKey());
        if (t != null) {
            return t.doTransition(e);
        } else {
            return noTransitionHandler.apply(e);
        }
    }

    /*
     * Add ann outgoing transition to the state
     */
    void addTransition(Object k, Transition t) {
        transitions.put(k, t);
    }

    /*
     * Set the 'no transition available for this event' handler
     */
    void setNoTransitionHandler(Function<Event, State> noTransitionHandler) {
        this.noTransitionHandler = noTransitionHandler;
    }

    /*
     * run a sequence of code when the state is entered
     */
    void runEntryCode(FSM fsm) {
    }

}
