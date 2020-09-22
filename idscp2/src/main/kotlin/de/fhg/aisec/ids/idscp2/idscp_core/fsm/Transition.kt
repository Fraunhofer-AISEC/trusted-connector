package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import java.util.function.Function

/**
 * Transition class for State machine, provides a doTransition method
 * that returns the next state for a given event
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Transition(private val eventHandler: Function<Event, State?>) {
    fun doTransition(e: Event): State? {
        return eventHandler.apply(e)
    }
}