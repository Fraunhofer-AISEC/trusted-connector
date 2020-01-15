package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.messages.IDSCPv2.*;

public class Event {
    public enum EventType {
        IDSCP_MESSAGE,
        INTERNAL_CONTROL_MESSAGE
    }

    private Object key;
    private EventType type;
    private IdscpMessage idscpMessage;
    private InternalControlMessage controlMessage;

    public Event(Object key, InternalControlMessage controlMessage){
        this.key = key;
        this.type = EventType.INTERNAL_CONTROL_MESSAGE;
        this.controlMessage = controlMessage;
        this.idscpMessage = null;
    }

    public Event (Object key, IdscpMessage idscpMessage){
        this.key = key;
        this.type = EventType.IDSCP_MESSAGE;
        this.idscpMessage = idscpMessage;
        this.controlMessage = null;
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
