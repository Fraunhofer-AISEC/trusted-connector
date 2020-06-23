package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage;

/**
 * An Event class for the Finite State Machine. Triggers a transition and holds
 * either an IdscpMessage or an InternalControlMessage, or both in special cases.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Event {
    public enum EventType {
        IDSCP_MESSAGE,
        INTERNAL_CONTROL_MESSAGE
    }

    private final Object key;
    private final EventType type;
    private final IdscpMessage idscpMessage;
    private final InternalControlMessage controlMessage;

    /*
     * Create an Event with an Internal Control Message
     */
    public Event(InternalControlMessage controlMessage) {
        this.key = controlMessage.getValue();
        this.type = EventType.INTERNAL_CONTROL_MESSAGE;
        this.controlMessage = controlMessage;
        this.idscpMessage = null;
    }

    /*
     * Create an Event with an Idscpv2 Message
     */
    public Event(IdscpMessage idscpMessage) {
        this.key = idscpMessage.getMessageCase().getNumber();
        this.type = EventType.IDSCP_MESSAGE;
        this.idscpMessage = idscpMessage;
        this.controlMessage = null;
    }

    /*
     * Create a event for outgoing RatProver and RatVerifier messages
     *
     * throws an IllegalStateException if this event is requested for other purposes
     */
    public Event(InternalControlMessage controlMessage, IdscpMessage idscpMessage) {
        if (controlMessage.equals(InternalControlMessage.RAT_PROVER_MSG) ||
                controlMessage.equals(InternalControlMessage.RAT_VERIFIER_MSG)) {
            this.key = controlMessage.getValue();
            this.type = EventType.INTERNAL_CONTROL_MESSAGE;
            this.idscpMessage = idscpMessage;
            this.controlMessage = controlMessage;
        } else {
            throw new IllegalStateException("This constructor must only be used by RAT_PROVER and " +
                    "RAT_VERIFIER for message passing");
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "key=" + key +
                ", type=" + type +
                ", idscpMessage=" + idscpMessage +
                ", controlMessage=" + controlMessage +
                '}';
    }

    //
    // Getter methods
    //

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
