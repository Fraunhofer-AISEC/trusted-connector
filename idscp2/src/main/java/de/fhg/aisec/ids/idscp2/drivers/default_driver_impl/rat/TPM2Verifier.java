package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierInstance;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;

public class TPM2Verifier extends Thread implements RatVerifierInstance {

    private FsmListener fsmListener;
    private boolean successful = false;
    private boolean running = true;
    private final Object resultAvailableLock = new Object();

    TPM2Verifier(FsmListener fsmListener){
        this.fsmListener = fsmListener;
    }

    @Override
    public void delegate(IDSCPv2.IdscpMessage message) {
        successful = true;
        synchronized (resultAvailableLock) {
            resultAvailableLock.notify();
        }
    }

    @Override
    public void terminate() {
        running = false;
    }

    @Override
    public void restart() {

    }

    @Override
    public void run(){
        while (running){
            synchronized (resultAvailableLock) {
                try {
                    resultAvailableLock.wait();
                    if (successful){
                        fsmListener.onControlMessage(InternalControlMessage.RAT_VERIFIER_OK);
                    } else {
                        fsmListener.onControlMessage(InternalControlMessage.RAT_VERIFIER_FAILED);
                    }
                    running = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
