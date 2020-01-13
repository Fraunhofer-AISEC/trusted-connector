package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IDSCPv2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class FSM implements FsmListener{
    private static final Logger LOG = LoggerFactory.getLogger(FSM.class);

    /*  -----------   IDSCPv2 Protocol States   ---------- */
    private static final State STATE_CLOSED = new State();
    private static final State STATE_WAIT_FOR_HELLO = new State();
    private static final State STATE_LISTEN = new State();
    private static final State STATE_WAIT_FOR_RAT = new State();
    private static final State STATE_WAIT_FOR_RAT_CLIENT = new State();
    private static final State STATE_WAIT_FOR_RAT_SERVER = new State();
    private static final State STATE_WAIT_FOR_DAT_AND_RAT_CLIENT = new State();
    private static final State STATE_WAIT_FOR_DAT_AND_RAT_SERVER = new State();
    private static final State STATE_ESTABLISHED = new State();
    /*  ----------------   end of states   --------------- */


    /*  -----------   IDSCPv2 Protocol Transitions   ---------- */

    /*  ----------------   end of transitions   --------------- */

    private State currentState;
    private SecureChannel secureChannel;
    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;
    private DapsDriver dapsDriver;
    private IdscpMsgListener listener = null;
    private CountDownLatch listenerLatch = new CountDownLatch(1);

    public FSM(SecureChannel secureChannel, RatProverDriver ratProver, RatVerifierDriver ratVerifier,
               DapsDriver dapsDriver){

        this.secureChannel = secureChannel;
        secureChannel.setFsm(this);

        this.ratProverDriver = ratProver;
        this.ratVerifierDriver = ratVerifier;
        this.dapsDriver = dapsDriver;

        currentState = STATE_CLOSED; //start state
    }

    public void startIdscpHandshake() throws IDSCPv2Exception {
        LOG.debug("Send IDSCP_HELLO");
        send(IDSCPv2.IdscpMessage.newBuilder()
                .setType(IDSCPv2.IdscpMessage.Type.IDSCP_HELLO)
                .setIdscpHello(IDSCPv2.IdscpHello.newBuilder().build()).build());
        //toDo verify security properties
        //toDo RAT
        LOG.debug("All security requirements are fulfilled for new idscp connection");
        currentState = STATE_ESTABLISHED;
    }

    public void send(IdscpMessage msg){
        secureChannel.send(msg.toByteArray());
    }

    public void sendError(String msg, String code){
        IdscpMessage idscpMessage = IdscpMessage.newBuilder()
                .setType(IdscpMessage.Type.IDSCP_ERROR)
                .setIdscpError(
                        IdscpError.newBuilder()
                                .setErrorMsg(msg)
                                .setErrorCode(code)
                ).build();
        send(idscpMessage);
    }

    @Override
    public void onMessage(byte[] data){
        try {
            IdscpMessage message = IdscpMessage.parseFrom(data);
            //toDo trigger event
            if (message.getType().equals(IdscpMessage.Type.IDSCP_DATA) && currentState.equals(STATE_ESTABLISHED)){
                listenerLatch.await();
                listener.onMessage(message);
            } else {
                //toDo fsm logic
            }

        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}. Send error message", data);
            sendError("Cannot parse raw data into IdscpMessage", "");
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public void terminate(){
        LOG.info("Close idscp connection");
        LOG.debug("Send IDSCP_CLOSE");
        IdscpMessage msg = IdscpMessage.newBuilder()
                .setType(IdscpMessage.Type.IDSCP_CLOSE)
                .setIdscpClose(IdscpClose.newBuilder().build()
                ).build();
        send(msg);
        LOG.debug("Close secure channel");
        secureChannel.close();
    }

    public boolean isConnected(){
        return currentState.equals(STATE_ESTABLISHED);
    }

    public void registerMessageListener(IdscpMsgListener listener){
        this.listener = listener;
        listenerLatch.countDown();
    }

    public void setEndpointConnectionId(String id){
        this.secureChannel.setEndpointConnectionId(id);
    }
}
