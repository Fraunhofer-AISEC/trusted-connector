package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FsmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * A secureChannel which is the secure underlying basis of the IDSCPv2 protocol, that implements a secureChannelListener
 *
 * Developer API
 *
 * Methods:
 * void close()                                 to close the secureChannel
 * void send(IDSCPv2Message)                    to send an IDSCPv2Message as bytes via the secure channel
 * void onMessage(byte[] data)                  to receive new bytes via the secure channel
 * boolean isConnected()                        to check if the secure channel is still open
 * void registerMessageListener(IdscpMsgListener)   to register an idscpv2 message listener
 * void setEndpointConnectionId(String id)          to set the connectionId
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

    public void close(){
        endpoint.close();
    }

    public void send(byte[] msg){
        LOG.debug("Send message via secure channel");
        endpoint.send(msg);
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

    public boolean isConnected(){
        return endpoint.isConnected();
    }

    public void setEndpointConnectionId(String id){
        this.endpoint.setConnectionId(id);
        //deadlock is resolved
    }

    public void setFsm(FsmListener fsm) {
        this.fsm = fsm;
        fsmLatch.countDown();
    }
}
