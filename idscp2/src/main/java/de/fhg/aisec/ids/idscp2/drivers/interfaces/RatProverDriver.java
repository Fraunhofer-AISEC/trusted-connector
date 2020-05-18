package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract RatProverDriver class that creates a rat prover driver thread and proves itself to
 * the peer connector using remote attestation
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public abstract class RatProverDriver extends Thread{
    private static final Logger LOG = LoggerFactory.getLogger(RatProverDriver.class);

    protected boolean running = true;
    protected FsmListener fsmListener;

    /*
     * Delegate an IDSCP2 message to the RatProver driver
     */
    public void delegate(byte[] message){}

    /*
     * Terminate and cancel the RatProver driver
     */
    public void terminate() {
        running = false;
        this.interrupt();
    }

    public void setListener(FsmListener listener){
        fsmListener = listener;
    }

    public void setConfig(Object config){
        LOG.warn("Method 'setConfig' for RatProverDriver is not implemented");
    }
}
