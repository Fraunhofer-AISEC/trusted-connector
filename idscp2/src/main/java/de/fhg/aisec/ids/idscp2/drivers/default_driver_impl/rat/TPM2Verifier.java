package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TPM2Verifier extends RatVerifierDriver {
    private static final Logger LOG = LoggerFactory.getLogger(TPM2Verifier.class);

    private BlockingQueue<IDSCPv2.IdscpMessage> queue = new LinkedBlockingQueue<>();

    public TPM2Verifier(){
        super();
    }

    @Override
    public void delegate(IDSCPv2.IdscpMessage message) {
        queue.add(message);
        LOG.debug("Delegated to Verifier");
    }

    @Override
    public void run(){
        int countDown = 2;
        while (true){
            try {
                sleep(2000);
                LOG.debug("Verifier waits");
                IDSCPv2.IdscpMessage m = queue.take();
                LOG.debug("Verifier receives, send something");
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG,
                        IdscpMessageFactory.getIdscpRatProverMessage());
                if (--countDown == 0)
                    break;
            } catch (InterruptedException e) {
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
                return;
            }
        }
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, null);
    }
}
