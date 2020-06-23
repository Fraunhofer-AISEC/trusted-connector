package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A Timer Thread that triggers timeouts in the fsm
 * The thread will only trigger the fsm if it has the fsm lock and the timeout
 * was not canceled before
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TimerThread extends Thread {

    private volatile boolean canceled = false;
    private final long delay; //timeout delay in seconds
    private final Runnable timeoutHandler; //timeout handler routine
    private final ReentrantLock fsmIsBusy; //lock for the fsm

    TimerThread(long delay, Runnable timeoutHandler, ReentrantLock fsmIsBusy) {
        this.delay = delay;
        this.timeoutHandler = timeoutHandler;
        this.fsmIsBusy = fsmIsBusy;
    }

    /*
     * Run the timer thread that sleeps the number of timeout delay in ms
     * if the timeout was not canceled during the sleep, the thread will request
     * the fsm lock and will then check once again, if the timeout was canceled
     * by another transition during this process to avoid triggering timeout
     * transitions for canceled timers.
     * If the timout was not canceled so far, the timer thread calls a timeout handler
     * routine, that triggers the timeout transition in the fsm
     */
    public void run() {
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {
            if (!canceled) {
                Thread.currentThread().interrupt();
            }
        }

        if (canceled) {
            return;
        }

        fsmIsBusy.lock();
        try {
            if (!canceled) {
                timeoutHandler.run();
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    /*
     * A method to stop the execution of the timer thread and cancel the timeout
     */
    public void safeStop() {
        canceled = true;
        this.interrupt();
    }
}
