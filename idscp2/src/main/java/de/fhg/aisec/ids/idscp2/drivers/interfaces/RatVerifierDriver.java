package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract RatVerifierDriver class that creates a rat verifier driver thread and verifier the
 * peer connector using remote attestation
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public abstract class RatVerifierDriver extends Thread{
    private static final Logger LOG = LoggerFactory.getLogger(RatVerifierDriver.class);

    protected boolean running = true;
    protected FsmListener fsmListener;

    /*
     * Delegate the IDSCPv2 message to the RatVerifier driver
     */
    public void delegate(IdscpMessage message){}

    /*
     * Terminate and cancel the RatVerifier driver
     */
    public void terminate() {
        running = false;
    }

    public void setListener(FsmListener listener){
        fsmListener = listener;
    }

    public void setConfig(Object config) {
        LOG.warn("Method 'setConfig' for RatVerifierDriver is not implemented");
    }
}
