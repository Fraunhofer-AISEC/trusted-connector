package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RatVerifierDummy extends RatVerifierDriver {
    private static final Logger LOG = LoggerFactory.getLogger(RatVerifierDummy.class);

    private BlockingQueue<IDSCPv2.IdscpMessage> queue = new LinkedBlockingQueue<>();

    public RatVerifierDummy(){
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
                sleep(1000);
                LOG.debug("Verifier waits");
                IDSCPv2.IdscpMessage m = queue.take();
                LOG.debug("Verifier receives, send something");
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG,
                        IdscpMessageFactory.getIdscpRatVerifierMessage("test".getBytes()));
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
