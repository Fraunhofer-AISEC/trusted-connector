package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A Timer class that provides an API to the FSN to start and cancel timeout threads
 * The timer ensures that no canceled timer is able to trigger a timeout transitions
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Timer {

    private TimerThread thread = null;
    private final ReentrantLock fsmIsBusy;
    private final ReentrantLock mutex = new ReentrantLock(true);
    private final Runnable timeoutHandler;

    Timer(ReentrantLock fsmIsBusy, Runnable timeoutHandler) {
        this.fsmIsBusy = fsmIsBusy;
        this.timeoutHandler = timeoutHandler;
    }

    void resetTimeout(long delay) {
        cancelTimeout();
        start(delay);
    }

    /*
     * Start a timer thread that triggers the timeout handler routine after a given timout delay
     */
    public void start(long delay) {
        mutex.lock();
        thread = new TimerThread(delay, timeoutHandler, fsmIsBusy);
        thread.start();
        mutex.unlock();
    }

    /*
     * Cancel the current timer thread
     */
    void cancelTimeout() {
        mutex.lock();
        if (thread != null) {
            thread.safeStop();
            thread = null;
        }
        mutex.unlock();
    }
}

