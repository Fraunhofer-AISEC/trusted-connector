package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

/**
 * A secureChannel which is the secure underlying basis of the IDSCPv2 protocol,
 * that implements a secureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class SecureChannel implements SecureChannelListener {
    private static final Logger LOG = LoggerFactory.getLogger(SecureChannel.class);

    private SecureChannelEndpoint endpoint;
    private CountDownLatch fsmLatch = new CountDownLatch(1);
    private FsmListener fsm = null;

    public SecureChannel(SecureChannelEndpoint secureChannelEndpoint){
        this.endpoint = secureChannelEndpoint;
    }

    /*
     * close the secure channel forever
     */
    public void close(){
        endpoint.close();
    }

    /*
     * Send data via the secure channel endpoint to the peer connector
     *
     * return true if the data were sent successfully, else false
     */
    public boolean send(byte[] msg){
        LOG.debug("Send message via secure channel");
        return endpoint.send(msg);
    }

    @Override
    public void onMessage(byte[] data){
        LOG.debug("New raw data were received via the secure channel");
        try {
            fsmLatch.await();
            fsm.onMessage(data);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onError() {
        //tell fsm an error occurred in secure channel
        try {
            fsmLatch.await();
            fsm.onError();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onClose() {
        //tell fsm secure channel received EOF
        try {
            fsmLatch.await();
            fsm.onClose();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected(){
        return endpoint.isConnected();
    }

    /*
     * set the corresponding finite state machine
     */
    public void setFsm(FsmListener fsm) {
        this.fsm = fsm;
        fsmLatch.countDown();
    }
}
