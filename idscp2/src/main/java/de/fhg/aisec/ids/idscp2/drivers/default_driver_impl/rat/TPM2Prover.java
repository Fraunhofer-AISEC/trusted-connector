package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverInstance;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2.*;

public class TPM2Prover extends Thread implements RatProverInstance{

    private boolean running = true;
    private FsmListener fsmListener;
    private boolean successful = false;
    private final Object resultAvailableLock = new Object();


    TPM2Prover(FsmListener fsmListener){
        this.fsmListener = fsmListener;
    }

    @Override
    public void delegate(IdscpMessage message) {
        successful = true;
        synchronized (resultAvailableLock) {
            resultAvailableLock.notify();
        }
    }

    public void terminate(){
        running = false;
    }

    @Override
    public void run() {
        while (running){
            synchronized (resultAvailableLock) {
                try {
                    resultAvailableLock.wait();
                    if (successful){
                        fsmListener.onControlMessage(InternalControlMessage.RAT_PROVER_SUCCESSFUL);
                    } else {
                        fsmListener.onControlMessage(InternalControlMessage.RAT_PROVER_FAILED);
                    }
                    running = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
