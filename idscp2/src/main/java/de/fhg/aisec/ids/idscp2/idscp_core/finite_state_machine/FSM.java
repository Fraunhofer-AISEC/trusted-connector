package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class FSM {
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
    private CountDownLatch handshakeComplete = new CountDownLatch(1);

    public FSM(SecureChannel secureChannel){
        this.secureChannel = secureChannel;
        currentState = STATE_ESTABLISHED;
    }

    public void start(){
        LOG.debug("Send IDSCP_HELLO");
        secureChannel.send(IDSCPv2.IdscpMessage.newBuilder()
                .setType(IDSCPv2.IdscpMessage.Type.IDSCP_HELLO)
                .setIdscpHello(IDSCPv2.IdscpHello.newBuilder().build()).build());
        //toDo verify security properties
        //toDo RAT
        handshakeComplete.countDown();
    }

    public void delegate(IdscpMessage msg){

    }

    private void send(IdscpMessage msg){
        secureChannel.send(msg);
    }

    public boolean isConnected(){
        return currentState.equals(STATE_ESTABLISHED);
    }

    public CountDownLatch getHandshakeComplete() {
        return handshakeComplete;
    }
}
