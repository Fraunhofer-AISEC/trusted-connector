package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

/**
 * A State class for the Finite State Machine.
 * A state holds all outgoing transitions and a noTransitionHandler for events, that do not trigger
 * any outgoing available transition. Transitions are mapped with the event key.
 *
 * @author Leon Beckmann leon.beckmann@aisec.fraunhofer.de
 */
open class State {
    private val transitions = ConcurrentHashMap<Any, Transition>()
    private lateinit var noTransitionHandler: Function<Event, FSM.FsmResult>

    /*
     * A method for triggering a transition of the current state by a given event
     *
     * If no transition exists for the given event, the noTransitionHandler is triggered
     *
     * Returns a FSM result that contains the target state of the triggered transition (new current state of the fsm),
     * as well as the resulting code of the transition
     */
    fun feedEvent(e: Event): FSM.FsmResult {
        val t = transitions[e.key]
        return t?.doTransition(e) ?: noTransitionHandler.apply(e)
    }

    /*
     * Add an outgoing transition to the state
     */
    fun addTransition(k: Any, t: Transition) {
        transitions[k] = t
    }

    /*
     * Set the 'no transition available for this event' handler
     */
    fun setNoTransitionHandler(noTransitionHandler: Function<Event, FSM.FsmResult>) {
        this.noTransitionHandler = noTransitionHandler
    }

    /*
     * run a sequence of code when the state is entered
     */
    open fun runEntryCode(fsm: FSM) {}
}