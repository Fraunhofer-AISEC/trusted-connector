package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A State class for the Finite State Machine. A state holds all outgoing transitions and a noTransitionHandler for
 * events, that do not trigger any outgoing available transition. Transitions are mapped with the event key
 *
 * @author Leon Beckmann leon.beckmann@aisec.fraunhofer.de
 */
public class State {

    private ConcurrentHashMap<Object,Transition> transitions = new ConcurrentHashMap<>();
    private Function<Event,State> noTransitionHandler = null;

    State feedEvent(Event e){
        Transition t = transitions.get(e.getKey());
        if (t != null){
            return t.doTransition(e);
        } else {
            return noTransitionHandler.apply(e);
        }
    }

    void addTransition(Object k, Transition t){
        transitions.put(k,t);
    }

    void setNoTransitionHandler(Function<Event, State> noTransitionHandler) {
        this.noTransitionHandler = noTransitionHandler;
    }
/*
    protected void runEntryCode(){

    }

    protected void runExitCode(){

    }

 */
}
