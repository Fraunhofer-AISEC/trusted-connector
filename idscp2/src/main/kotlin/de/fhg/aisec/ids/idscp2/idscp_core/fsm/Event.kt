package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage

/**
 * An Event class for the Finite State Machine. Triggers a transition and holds
 * either an IdscpMessage or an InternalControlMessage, or both in special cases.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Event {
    enum class EventType {
        IDSCP_MESSAGE, INTERNAL_CONTROL_MESSAGE
    }

    val key: Any?
    val type: EventType
    lateinit var idscpMessage: IdscpMessage
        private set
    lateinit var controlMessage: InternalControlMessage
        private set

    /**
     * Create an event with an Internal Control Message
     */
    constructor(controlMessage: InternalControlMessage) {
        key = controlMessage.value
        type = EventType.INTERNAL_CONTROL_MESSAGE
        this.controlMessage = controlMessage
    }

    /**
     * Create an event with an Idscpv2 Message
     */
    constructor(idscpMessage: IdscpMessage) {
        key = idscpMessage.messageCase.number
        type = EventType.IDSCP_MESSAGE
        this.idscpMessage = idscpMessage
    }

    /**
     * Create an event for outgoing RatProver, RatVerifier, IdscpData messages
     *
     * throws an IllegalStateException if this event is requested for other purposes
     */
    constructor(controlMessage: InternalControlMessage, idscpMessage: IdscpMessage) {
        if (controlMessage == InternalControlMessage.RAT_PROVER_MSG
                || controlMessage == InternalControlMessage.RAT_VERIFIER_MSG
                || controlMessage == InternalControlMessage.SEND_DATA) {
            key = controlMessage.value
            type = EventType.INTERNAL_CONTROL_MESSAGE
            this.idscpMessage = idscpMessage
            this.controlMessage = controlMessage
        } else {
            throw IllegalStateException("This constructor must only be used by RAT_PROVER, " +
                    "RAT_VERIFIER for message passing and for SEND_DATA, encountered $controlMessage")
        }
    }

    override fun toString(): String {
        return "Event{" +
                "key=" + key +
                ", type=" + type +
                ", idscpMessage=" + if(::idscpMessage.isInitialized) idscpMessage else null +
                ", controlMessage=" + if(::controlMessage.isInitialized) controlMessage else null +
                '}'
    }
}