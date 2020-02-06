package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.locks.ReentrantLock;

public class TimerThread extends Thread{

    private volatile boolean canceled = false;
    private int delay;
    private final Runnable timeoutHandler;
    private final ReentrantLock fsmIsBusy;

    TimerThread(int delay, Runnable timeoutHandler, ReentrantLock fsmIsBusy){
        this.delay = delay;
        this.timeoutHandler = timeoutHandler;
        this.fsmIsBusy = fsmIsBusy;
    }

    public void run(){
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
            if (!canceled){
                timeoutHandler.run();
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    public void safeStop(){
        canceled = true;
        this.interrupt();
    }
}
