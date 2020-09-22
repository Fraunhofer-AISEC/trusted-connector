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
    private var noTransitionHandler: Function<Event, State>? = null

    /*
     * A method for triggering aa transition of the current state by a given event
     *
     * If no transition exists for the given event, the noTransitionHandler is triggered
     *
     * Returns the target state of the triggered transition (new current state of the fsm)
     */
    fun feedEvent(e: Event): State? {
        val t = transitions[e.key]
        return if (t != null) {
            t.doTransition(e)
        } else {
            noTransitionHandler!!.apply(e)
        }
    }

    /*
     * Add ann outgoing transition to the state
     */
    fun addTransition(k: Any, t: Transition) {
        transitions[k] = t
    }

    /*
     * Set the 'no transition available for this event' handler
     */
    fun setNoTransitionHandler(noTransitionHandler: Function<Event, State>?) {
        this.noTransitionHandler = noTransitionHandler
    }

    /*
     * run a sequence of code when the state is entered
     */
    open fun runEntryCode(fsm: FSM) {}
}