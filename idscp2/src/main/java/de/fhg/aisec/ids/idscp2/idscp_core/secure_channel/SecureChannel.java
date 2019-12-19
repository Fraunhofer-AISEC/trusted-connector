package de.fhg.aisec.ids.idscp2.idscp_core.secure_channel;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;

import java.util.Arrays;

/**
 * An interface for the secureChannel (e.g. TLS), which is the secure underlying basis of the IDSCPv2 protocol
 *
 * Developer API
 *
 * Constructors depends on the implementation
 *
 * Methods:
 * void close()                                 to close the secureChannel to the other secure endpoint
 * void send(IDSCPv2Message)                    to send an IDSCPv2Message as bytes via the secure channel
 * void onMessage(byte[] data)                  to receive new bytes via the secure channel
 * boolean isConnected()                        to check if the secure channel is still open
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class SecureChannel implements SecureChannelListener {

    private SecureChannelEndpoint endpoint;
    private IdscpMsgListener listener = null;

    public SecureChannel(SecureChannelEndpoint secureChannelEndpoint){
        this.endpoint = secureChannelEndpoint;
    }

    public void close(){
        endpoint.close();
    }

    public void send(IdscpMessage msg){
        endpoint.send(msg.toByteArray());
    }

    @Override
    public void onMessage(byte[] data){
        System.out.println(Arrays.toString(data));
        try {
            IdscpMessage message = IdscpMessage.parseFrom(data);
            listener.onMessage(message);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return endpoint.isConnected();
    }

    public void registerMessageListener(IdscpMsgListener listener){
        this.listener = listener;
    }

    public void setEndpointConnectionId(String id){
        this.endpoint.setConnectionId(id);
    }
}
