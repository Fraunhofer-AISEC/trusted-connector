package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2.*;

public class TPM2Prover extends RatProverDriver {

    public TPM2Prover(){
        super();
    }

    @Override
    public void delegate(IdscpMessage message) {
        //todo blocking
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);

    }

}
