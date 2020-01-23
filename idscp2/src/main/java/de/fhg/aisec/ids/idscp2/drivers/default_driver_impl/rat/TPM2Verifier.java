package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;

public class TPM2Verifier extends RatVerifierDriver {

    public TPM2Verifier(){
        super();
    }

    @Override
    public void delegate(IDSCPv2.IdscpMessage message) {
        //toDo blocking
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG,
                IdscpMessageFactory.getIdscpRatVerifierMessage());
        successful = true;
        synchronized (resultAvailableLock) {
            resultAvailableLock.notify();
        }
    }

    @Override
    public void run(){
        while (running){
            synchronized (resultAvailableLock) {
                /*try {
                    resultAvailableLock.wait();
                    if (successful){
                        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, null);
                    } else {
                        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
                    }
                    running = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, null);
            }
        }
    }
}
