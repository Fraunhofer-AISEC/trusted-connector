package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import static java.util.concurrent.TimeUnit.*;

public class Timer {

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> timer;
    private boolean isLocked = false;
    private boolean isExecuting = false;
    private Runnable timeoutHandler;

    public Timer(Runnable timeoutHandler){
        this.timeoutHandler = timeoutHandler;
    }

    public void cancelTimeout() {
        if (!isLocked) {
            if (isExecuting) {
                timer.cancel(true);
            }
        }
    }

    public boolean resetTimeout(int delay){
        if (!isLocked){
            if (isExecuting){
                timer.cancel(true);
            }
            timer = executor.schedule(timeoutHandler, delay, SECONDS);
            return true;
        }
        return false;
    }

    public void stop(){
        if (!isLocked){
            executor.shutdownNow();
            isLocked = true;
        }
    }

}
