package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import java.util.concurrent.locks.ReentrantLock

/**
 * A Timer class that provides an API to the FSN to start and cancel timeout threads
 * The timer ensures that no canceled timer is able to trigger a timeout transitions
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Timer internal constructor(private val fsmIsBusy: ReentrantLock, private val timeoutHandler: Runnable) {
    private var thread: TimerThread? = null
    private val mutex = ReentrantLock(true)
    fun resetTimeout(delay: Long) {
        cancelTimeout()
        start(delay)
    }

    /*
     * Start a timer thread that triggers the timeout handler routine after a given timout delay
     */
    fun start(delay: Long) {
        mutex.lock()
        thread = TimerThread(delay, timeoutHandler, fsmIsBusy)
        thread!!.start()
        mutex.unlock()
    }

    /*
     * Cancel the current timer thread
     */
    fun cancelTimeout() {
        mutex.lock()
        if (thread != null) {
            thread!!.safeStop()
            thread = null
        }
        mutex.unlock()
    }
}