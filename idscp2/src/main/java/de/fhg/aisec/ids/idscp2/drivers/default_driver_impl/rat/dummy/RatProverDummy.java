package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RatProver dummy that exchanges rat messages with a remote RatVerifier
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class RatProverDummy extends RatProverDriver {
    private static final Logger LOG = LoggerFactory.getLogger(RatProverDummy.class);

    private BlockingQueue<IdscpMessage> queue = new LinkedBlockingQueue<>();

    public RatProverDummy(){
        super();
    }

    @Override
    public void delegate(IdscpMessage message) {
        queue.add(message);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delegated to prover");
        }
    }

    @Override
    public void run() {
        int countDown = 2;
        while (running){
            try {
                sleep(1000);
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG,
                        IdscpMessageFactory.getIdscpRatProverMessage("test".getBytes()));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Prover waits");
                }
                IDSCPv2.IdscpMessage m = queue.take();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Prover receives, send something");
                }
                if (--countDown == 0)
                    break;
            } catch (InterruptedException e) {
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_FAILED, null);
                return;
            }
        }
        fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_OK, null);
    }
}
