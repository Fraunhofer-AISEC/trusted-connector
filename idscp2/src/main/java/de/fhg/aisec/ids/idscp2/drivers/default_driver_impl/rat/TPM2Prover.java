package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2.*;

public class TPM2Prover extends RatProverDriver {

    public TPM2Prover(){
        super();
    }

    @Override
    public void delegate(IdscpMessage message) {
        //toDo blocking
        successful = true;
        synchronized (resultAvailableLock) {
            resultAvailableLock.notify();
        }
    }

    @Override
    public void run() {
        /*fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG,
                IdscpMessageFactory.getIdscpRatProverMessage());*/
        while (running){
            synchronized (resultAvailableLock) {
                /*try {
                    resultAvailableLock.wait();
                    if (successful){
                        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);
                    } else {
                        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
                    }
                    running = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);
            }
        }
    }
}
