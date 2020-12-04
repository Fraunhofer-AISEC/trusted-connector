package de.fhg.aisec.ids.idscp2.idscp_core.fsm

/**
 * Transition class for State machine, provides a doTransition method
 * that returns the fsm result containing of the next state for a given event and the result code
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Transition(private val eventHandler: (Event) -> FSM.FsmResult) {
    fun doTransition(e: Event): FSM.FsmResult {
        return eventHandler(e)
    }
}