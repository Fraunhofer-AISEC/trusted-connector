package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import java.util.concurrent.locks.ReentrantLock

/**
 * A Timer Thread that triggers timeouts (ms) in the fsm
 * The thread will only trigger the fsm if it has the fsm lock and the timeout
 * was not canceled before
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TimerThread internal constructor(//timeout delay in milliseconds
        private val delay: Long, //timeout handler routine
        private val timeoutHandler: Runnable, //lock for the fsm
        private val fsmIsBusy: ReentrantLock) : Thread() {
    @Volatile
    private var canceled = false

    /*
     * Run the timer thread that sleeps the number of timeout delay in ms
     * if the timeout was not canceled during the sleep, the thread will request
     * the fsm lock and will then check once again, if the timeout was canceled
     * by another transition during this process to avoid triggering timeout
     * transitions for canceled timers.
     * If the timout was not canceled so far, the timer thread calls a timeout handler
     * routine, that triggers the timeout transition in the fsm
     */
    override fun run() {
        try {
            sleep(delay)
        } catch (e: InterruptedException) {
            if (!canceled) {
                currentThread().interrupt()
            }
        }
        if (canceled) {
            return
        }
        fsmIsBusy.lock()
        try {
            if (!canceled) {
                timeoutHandler.run()
            }
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /*
     * A method to stop the execution of the timer thread and cancel the timeout
     */
    fun safeStop() {
        canceled = true
        interrupt()
    }
}