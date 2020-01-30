package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import de.fhg.aisec.ids.messages.IDSCPv2;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A RatProver dummy, that sends two incoming messages from the remote rat verifier and also sends two messages
 */
public class TPM2Prover extends RatProverDriver {
    private static final Logger LOG = LoggerFactory.getLogger(TPM2Prover.class);

    private BlockingQueue<IdscpMessage> queue = new LinkedBlockingQueue<>();

    public TPM2Prover(){
        super();
    }

    @Override
    public void delegate(IdscpMessage message) {
        queue.add(message);
        LOG.debug("Delegated to prover");
    }

    @Override
    public void run() {
        int countDown = 2;
        while (true){
            try {
                sleep(1000);
                fsmListener.onRatProverMessage(InternalControlMessage.RAT_PROVER_MSG,
                        IdscpMessageFactory.getIdscpRatProverMessage());
                LOG.debug("Prover waits");
                IDSCPv2.IdscpMessage m = queue.take();
                LOG.debug("Prover receives, send something");
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
