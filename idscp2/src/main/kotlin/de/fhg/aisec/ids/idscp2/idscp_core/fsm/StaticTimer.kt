package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import java.util.concurrent.locks.ReentrantLock

/**
 * A StaticTimer class that provides an API to the FSM to start and cancel timeout threads
 * with a fixed timeout delay (in ms)
 * The timer ensures that no canceled timer is able to trigger a timeout transitions
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StaticTimer internal constructor(private val fsmIsBusy: ReentrantLock,
                                       private val timeoutHandler: Runnable,
                                       private val delay: Long) {
    private var thread: TimerThread? = null
    private val mutex = ReentrantLock(true)

    fun resetTimeout() {
        cancelTimeout()
        start()
    }

    /*
     * Start a timer thread that triggers the timeout handler routine after the static delay
     */
    fun start() {
        mutex.lock()
        thread = TimerThread(delay, timeoutHandler, fsmIsBusy).also { it.start() }
        mutex.unlock()
    }

    /*
     * Cancel the current timer thread
     */
    fun cancelTimeout() {
        mutex.lock()
        thread?.safeStop()
        thread = null
        mutex.unlock()
    }
}