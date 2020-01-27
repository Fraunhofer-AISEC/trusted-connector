package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

public class TimerThread extends Thread{

    private volatile boolean canceled = false;
    private int delay;
    private final Runnable timeoutHandler;
    private final Object lock;

    TimerThread(int delay, Runnable timeoutHandler, Object lock){
        this.delay = delay;
        this.timeoutHandler = timeoutHandler;
        this.lock = lock;
    }

    public void run(){
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (canceled)
            return;

        synchronized (lock){
            if (!canceled){
                timeoutHandler.run();
            }
        }
    }

    public void safeStop(){
        canceled = true;
    }
}
