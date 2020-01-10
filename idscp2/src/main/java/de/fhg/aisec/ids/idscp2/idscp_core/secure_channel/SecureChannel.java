package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
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
    private IdscpMsgListener listener = null;
    private CountDownLatch listenerLatch = new CountDownLatch(1);
    private FSM fsm;

    public SecureChannel(SecureChannelEndpoint secureChannelEndpoint){
        this.endpoint = secureChannelEndpoint;
        this.fsm = new FSM(this);
    }

    public void close(){
        endpoint.close();
    }

    public void send(IdscpMessage msg){
        LOG.debug("Send idscp message via secure channel");
        endpoint.send(msg.toByteArray());
    }

    @Override
    public void onMessage(byte[] data){
        LOG.debug("New raw data were received via the secure channel");
        try {
            IdscpMessage message = IdscpMessage.parseFrom(data);
            if (fsm.isConnected()){
                listenerLatch.await();
                listener.onMessage(message);
            } else {
                fsm.delegate(message);
            }
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}. Send error message", data);
            send(
                    IdscpMessage.newBuilder()
                        .setType(IdscpMessage.Type.IDSCP_ERROR)
                        .setIdscpError(IdscpError.newBuilder().setErrorMsg("Cannot parse raw data into IdscpMessage"))
                        .build()
            );
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected(){
        return endpoint.isConnected();
    }

    public void registerMessageListener(IdscpMsgListener listener){
        this.listener = listener;
        listenerLatch.countDown();
    }

    public void setEndpointConnectionId(String id){
        this.endpoint.setConnectionId(id);
    }

    public void startIdscpHandshake() throws IDSCPv2Exception {
        fsm.start();
        try {
            fsm.getHandshakeComplete().await();
        } catch (InterruptedException e) {
            throw new IDSCPv2Exception("IDSCPv2 handshake failed");
        }

        if (!fsm.isConnected()){
            throw new IDSCPv2Exception("IDSCPv2 handshake failed");
        }

        LOG.info("Idscpv2 handshake successful");
    }


}
