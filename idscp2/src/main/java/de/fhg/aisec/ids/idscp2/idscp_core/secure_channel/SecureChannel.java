package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * A secureChannel which is the secure underlying basis of the IDSCP2 protocol,
 * that implements a secureChannelListener
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class SecureChannel implements SecureChannelListener {
    private static final Logger LOG = LoggerFactory.getLogger(SecureChannel.class);

    private final SecureChannelEndpoint endpoint;
    private final CompletableFuture<FsmListener> fsmPromise = new CompletableFuture<>();

    public SecureChannel(SecureChannelEndpoint secureChannelEndpoint) {
        this.endpoint = secureChannelEndpoint;
    }

    /*
     * close the secure channel forever
     */
    public void close() {
        endpoint.close();
    }

    /*
     * Send data via the secure channel endpoint to the peer connector
     *
     * return true if the data has been sent successfully, else false
     */
    public boolean send(byte[] msg) {
        LOG.debug("Send message via secure channel");
        return endpoint.send(msg);
    }

    @Override
    public void onMessage(byte[] data) {
        LOG.debug("New raw data has been received via the secure channel");
        fsmPromise.thenAccept(fsmListener -> fsmListener.onMessage(data));
    }

    @Override
    public void onError() {
        // Tell fsm an error occurred in secure channel
        fsmPromise.thenAccept(FsmListener::onError);
    }

    @Override
    public void onClose() {
        // Tell fsm secure channel received EOF
        fsmPromise.thenAccept(FsmListener::onClose);
    }

    public boolean isConnected() {
        return endpoint.isConnected();
    }

    /*
     * set the corresponding finite state machine
     */
    public void setFsm(FsmListener fsm) {
        fsmPromise.complete(fsm);
    }
}
