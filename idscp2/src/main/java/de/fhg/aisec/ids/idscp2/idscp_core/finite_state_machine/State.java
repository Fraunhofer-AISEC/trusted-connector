package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.function.Function;

public class State {

    private Function<Event, State> eventHandler;

    public State feedEvent(Event e){
        return eventHandler.apply(e);
    }

    public void setEventHandler(Function<Event, State> eventHandler) {
        this.eventHandler = eventHandler;
    }
}
