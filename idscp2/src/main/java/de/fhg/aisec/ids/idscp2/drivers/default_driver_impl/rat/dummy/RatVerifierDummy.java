package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.dummy;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.InternalControlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A RatVerifier dummy that exchanges messages with a remote RatProver dummy
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class RatVerifierDummy extends RatVerifierDriver {
    private static final Logger LOG = LoggerFactory.getLogger(RatVerifierDummy.class);

    private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    public RatVerifierDummy(){
        super();
    }

    @Override
    public void delegate(byte[] message) {
        queue.add(message);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Delegated to Verifier");
        }
    }

    @Override
    public void run(){
        int countDown = 2;
        while (running){
            try {
                sleep(1000);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Verifier waits");
                }
                byte[] m = queue.take();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Verifier receives, send something");
                }
                fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_MSG,
                        "test".getBytes());
                if (--countDown == 0)
                    break;
            } catch (InterruptedException e) {
                if (this.running) {
                    fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_FAILED, null);
                }
                return;
            }
        }
        fsmListener.onRatVerifierMessage(InternalControlMessage.RAT_VERIFIER_OK, null);
    }
}
