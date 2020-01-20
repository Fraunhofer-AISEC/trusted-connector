package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.messages.IDSCPv2.*;

import javax.swing.*;

/**
 * An Event class for the Finite State Machine. Triggers a transition and holds either an idscpMessage or an
 * InternalControlMessage.
 *
 * @author Leon Beckmann leon.beckmann@aisec.fraunhofer.de
 */
public class Event {
    public enum EventType {
        IDSCP_MESSAGE,
        INTERNAL_CONTROL_MESSAGE
    }

    private Object key;
    private EventType type;
    private IdscpMessage idscpMessage;
    private InternalControlMessage controlMessage;

    public Event(InternalControlMessage controlMessage){
        this.key = controlMessage.getValue();
        this.type = EventType.INTERNAL_CONTROL_MESSAGE;
        this.controlMessage = controlMessage;
        this.idscpMessage = null;
    }

    public Event (IdscpMessage idscpMessage){
        this.key = idscpMessage.getMessageCase().getNumber();
        this.type = EventType.IDSCP_MESSAGE;
        this.idscpMessage = idscpMessage;
        this.controlMessage = null;
    }

    //for outgoing ratProver and ratVerifier messages
    public Event (InternalControlMessage controlMessage, IdscpMessage idscpMessage){
        if (controlMessage.equals(InternalControlMessage.RAT_PROVER_MSG) ||
                controlMessage.equals(InternalControlMessage.RAT_VERIFIER_MSG))
        {
            this.key = controlMessage.getValue();
            this.type = EventType.INTERNAL_CONTROL_MESSAGE;
            this.idscpMessage = idscpMessage;
            this.controlMessage = controlMessage;
        } else {
            throw new IllegalStateException("This constructor must only be by RAT_PROVER and " +
                    "RAT_VERIFIER for message passing");
        }
    }


    public Object getKey() {
        return key;
    }

    public EventType getType() {
        return type;
    }

    public IdscpMessage getIdscpMessage() {
        return idscpMessage;
    }

    public InternalControlMessage getControlMessage() {
        return controlMessage;
    }
}
