package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

public class Transition {

    private final State startState;
    private final State endState;

    public Transition(State startState, State endState){
        this.startState = startState;
        this.endState = endState;
    }

}
