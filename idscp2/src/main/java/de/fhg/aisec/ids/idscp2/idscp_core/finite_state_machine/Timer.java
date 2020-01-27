package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.locks.ReentrantLock;

public class Timer {

    //toDo real time FIFO synchronization for whole state machine

    private TimerThread thread = null;
    private final Object lock;
    private final ReentrantLock mutex = new ReentrantLock();
    private final Runnable timeoutHandler;

    Timer(Object lock, Runnable timeoutHandler){
        this.lock = lock;
        this.timeoutHandler = timeoutHandler;
    }

    void resetTimeout(int delay){
        cancelTimeout();
        start(delay);
    }

    public void start(int delay){
        mutex.lock();
        thread = new TimerThread(delay, timeoutHandler, lock);
        thread.start();
        mutex.unlock();
    }

    void cancelTimeout(){
        mutex.lock();
        if (thread != null){
            thread.safeStop();
            thread = null;
        }
        mutex.unlock();
    }
}

