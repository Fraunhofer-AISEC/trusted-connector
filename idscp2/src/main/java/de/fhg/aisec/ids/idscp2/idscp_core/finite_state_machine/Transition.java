package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.function.Function;

/**
 * Transition class for State machine, provides a doTransition method
 * that returns the next state for a given event
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Transition {
    private Function<Event,State> eventHandler;

    Transition(Function<Event,State> eventHandler){
        this.eventHandler = eventHandler;
    }

    State doTransition(Event e){
        return eventHandler.apply(e);
    }
}
