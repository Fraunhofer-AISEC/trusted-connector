package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.Event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class FSM implements FsmListener{
    private static final Logger LOG = LoggerFactory.getLogger(FSM.class);

    /*  -----------   IDSCPv2 Protocol States   ---------- */
    private final State STATE_CLOSED = new State();
    private final State STATE_WAIT_FOR_HELLO = new State();
    private final State STATE_WAIT_FOR_RAT= new State();
    private final State STATE_WAIT_FOR_RAT_CLIENT = new State();
    private final State STATE_WAIT_FOR_RAT_SERVER = new State();
    private final State STATE_WAIT_FOR_DAT_AND_RAT_CLIENT = new State();
    private final State STATE_WAIT_FOR_DAT_AND_RAT_SERVER = new State();
    private final State STATE_ESTABLISHED = new State();
    /*  ----------------   end of states   --------------- */


    private State currentState;
    private final State initialState = STATE_CLOSED;
    private SecureChannel secureChannel;
    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;
    private DapsDriver dapsDriver;
    private IdscpMsgListener listener = null;
    private final CountDownLatch listenerLatch = new CountDownLatch(1);
    private IdscpMessage cachedIdscpHello = null;
    private final Object idscpHandshakeLock = new Object();
    private final Object fsmIsBusy = new Object();

    public FSM(SecureChannel secureChannel, RatProverDriver ratProver, RatVerifierDriver ratVerifier,
               DapsDriver dapsDriver){

        this.secureChannel = secureChannel;
        this.ratProverDriver = ratProver;
        this.ratVerifierDriver = ratVerifier;
        this.dapsDriver = dapsDriver;
        secureChannel.setFsm(this);

        /*  -----------   Protocol Transitions   ---------- */

        STATE_CLOSED.setEventHandler(
                /*--------------------------------
                * Transition description
                * --------------------------------
                * INTERNAL_CONTROL_MESSAGE.START_IDSCP_HANDSHAKE ---> send IDSCP_HELLO ---> STATE_WAIT_FOR_HELLO
                * INTERNAL_CONTROL_MESSAGE.IDSCP_STOP ---> send IDSCP_CLOSE ---> STATE_CLOSED
                * IDSCP_MESSAGE.IDSCP_HELLO ---> cache IDSCP_HELLO ---> STATE_CLOSED
                * ALL_OTHER_MESSAGES ---> STATE_CLOSED
                */
                e -> {
                    LOG.debug("STATE_CLOSED triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){
                        if (e.getControlMessage().equals(InternalControlMessage.START_IDSCP_HANDSHAKE)){
                            LOG.debug("Get DAT Token vom DAT_DRIVER");
                            //toDo this.dapsDriver.getToken();
                            LOG.debug("Send idscp hello");
                            IdscpMessage idscpHello = IdscpMessageFactory.
                                    getIdscpHelloMessage(new byte[]{}, new String[] {"a", "b"}, new String[] {"a", "b"});
                            this.send(idscpHello);
                            //toDo set handshake timeout
                            LOG.debug("Switch to state STATE_WAIT_FOR_HELLO");
                            return STATE_WAIT_FOR_HELLO;
                        }
                    } else { /* IDSCP_MESSAGE */
                        if (e.getIdscpMessage().hasIdscpHello()){
                            LOG.debug("Cache idscp hello message");
                            this.cachedIdscpHello = e.getIdscpMessage();
                        }
                    }

                    LOG.debug("Stay in state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return this.STATE_CLOSED;
                }
        );

        STATE_WAIT_FOR_HELLO.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 * INTERNAL_CONTROL_MESSAGE.ERROR ---> send IDSCP_CLOSE ---> STATE_CLOSED
                 * INTERNAL_CONTROL_MESSAGE.IDSCP_STOP ---> send idscp close ---> STATE_CLOSED
                 * IDSCP_CLOSE ---> STATE_CLOSED
                 * toDo IDSCP_HELLO ---> DAT Verification failed: send IDSCP_ERROR, send IDSCP_CLOSE ---> STATE_CLOSED
                 * IDSCP_HELLO ---> verify DAT, set DAT Timeout, start RAT P&V ---> STATE_WAIT_FOR_RAT
                 * ALL_OTHER_MESSAGES ---> STATE_WAIT_FOR_HELLO
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_HELLO triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){
                        if (e.getControlMessage().equals(InternalControlMessage.ERROR)){
                            LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                            send(IdscpMessageFactory.getIdscpCloseMessage());
                            notifyHandshakeCompleteLock();
                            return STATE_CLOSED;
                        }

                        if (e.getControlMessage().equals(InternalControlMessage.IDSCP_STOP)){
                            LOG.debug("Send IDSC_CLOSE");
                            send(IdscpMessageFactory.getIdscpCloseMessage());
                            notifyHandshakeCompleteLock();
                            return STATE_CLOSED;
                        }
                    } else { //IDSCP_MESSAGE
                        if (e.getIdscpMessage().hasIdscpClose()){
                            LOG.debug("");
                        } else if (e.getIdscpMessage().hasIdscpHello()) {
                            LOG.debug("Verify received DAT");
                            //toDo Dat verification
                            //toDo set DAT timeout
                            //toDo start RAT_PROVER
                            //toDo start RAT_VERIFIER
                            LOG.debug("Switch to state STATE_ESTABLISHED");
                            notifyHandshakeCompleteLock();
                            return STATE_ESTABLISHED;
                        }
                    }
                    LOG.debug("Stay in state STATE_WAIT_FOR_HELLO");
                    return this.STATE_WAIT_FOR_HELLO;
                }
        );

        STATE_WAIT_FOR_RAT.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_RAT triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){

                    } else { //IDSCP_MESSAGE

                    }
                    return this.STATE_WAIT_FOR_RAT;
                }
        );

        STATE_WAIT_FOR_RAT_CLIENT.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_RAT_CLIENT triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){

                    } else { //IDSCP_MESSAGE

                    }
                    return this.STATE_WAIT_FOR_RAT_CLIENT;
                }
        );

        STATE_WAIT_FOR_RAT_SERVER.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_RAT_SERVER triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){

                    } else { //IDSCP_MESSAGE

                    }
                    return this.STATE_WAIT_FOR_RAT_SERVER;
                }
        );

        STATE_WAIT_FOR_DAT_AND_RAT_CLIENT.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_DAT_AND_RAT_CLIENT triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){

                    } else { //IDSCP_MESSAGE

                    }
                    return STATE_WAIT_FOR_DAT_AND_RAT_CLIENT;
                }
        );

        STATE_WAIT_FOR_DAT_AND_RAT_SERVER.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 */
                e -> {
                    LOG.debug("STATE_WAIT_FOR_DAT_AND_RAT_SERVER triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){

                    } else { //IDSCP_MESSAGE

                    }
                    return STATE_WAIT_FOR_DAT_AND_RAT_SERVER;
                }
        );

        STATE_ESTABLISHED.setEventHandler(
                /*--------------------------------
                 * Transition description
                 * --------------------------------
                 * INTERNAL_CONTROL_MESSAGE.ERROR ---> send idscp error, send idscp close ---> STATE_CLOSED
                 * INTERNAL_CONTROL_MESSAGE.IDSCP_STOP ---> send idscp close ---> STATE_CLOSED
                 * toDo INTERNAL_CONTROL_MESSAGE.DAT_EXPIRED ---> send DAT_EXPIRED, set timeout ---> STATE_WAIT_FOR_DAT_AND_RAT_CLIENT
                 * IDSCP_MESSAGE.DATA ---> listener.onMessage() ---> STATE_ESTABLISHED
                 * IDSCP_MESSAGE.CLOSE ---> STATE_CLOSED
                 */
                e -> {
                    LOG.debug("STATE_ESTABLISHED triggered");
                    if (e.getType().equals(EventType.INTERNAL_CONTROL_MESSAGE)){
                        if (e.getControlMessage().equals(InternalControlMessage.ERROR)){
                            LOG.debug("Error occurred, send IDSCP_ERROR and IDSCP_CLOSE and close idscp connection");
                            send(IdscpMessageFactory.getIdscpErrorMessage("Error occurred", ""));
                            send(IdscpMessageFactory.getIdscpCloseMessage());
                            return STATE_CLOSED;
                        } else if (e.getControlMessage().equals(InternalControlMessage.IDSCP_STOP)){
                            send(IdscpMessageFactory.getIdscpCloseMessage());
                            LOG.debug("Close idscp connection and send IDSCP_CLOSE");
                            return STATE_CLOSED;
                        }
                    } else { //IDSCP_MESSAGE
                        IdscpMessage message = e.getIdscpMessage();
                        if (message.hasIdscpData()){
                            try {
                                this.listenerLatch.await();
                                this.listener.onMessage(message);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        } else if (message.hasIdscpClose()){
                            LOG.debug("Receive IDSCP_CLOSED");
                            try {
                                this.listenerLatch.await();
                                this.listener.onMessage(message);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                            LOG.debug("Switch to STATE_CLOSED");
                            return STATE_CLOSED;
                        }
                    }
                    LOG.debug("Stay in STATE_ESTABLISHED");
                    return STATE_ESTABLISHED;
                }
        );

        /*  ----------------   end of transitions   --------------- */

        //set initial state
        currentState = initialState;
    }

    @Override
    public void onMessage(byte[] data){
        //parse message and create new IDSCP Message Event, then pass it to current state and update new state
        try {
            IdscpMessage message = IdscpMessage.parseFrom(data);
            Event e = new Event(null, message);
            synchronized (fsmIsBusy){
                currentState = currentState.feedEvent(e);
            }
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}. Send error message", data);
            sendError("Cannot parse raw data into IdscpMessage", "");
        }
    }

    @Override
    public void onControlMessage(InternalControlMessage controlMessage) {
        //create Internal Control Message Event and pass it to current state and update new state
        Event e = new Event(null, controlMessage);
        synchronized (fsmIsBusy){
            currentState = currentState.feedEvent(e);
        }
    }

    public void terminate(){
        LOG.info("Close idscp connection");
        onControlMessage(InternalControlMessage.IDSCP_STOP);
        LOG.debug("Close secure channel");
        secureChannel.close();
    }

    public void startIdscpHandshake() throws IDSCPv2Exception {
        if (currentState.equals(STATE_CLOSED)){
            //trigger handshake init
            onControlMessage(InternalControlMessage.START_IDSCP_HANDSHAKE);

            //check if a idscpHello was already received and trigger next transition
            if (cachedIdscpHello != null){
                Event e = new Event(null, cachedIdscpHello);
                synchronized (fsmIsBusy){
                    currentState = currentState.feedEvent(e);
                }
            }

            try {
                //wait until handshake was successful or failed
                synchronized (idscpHandshakeLock) {
                    idscpHandshakeLock.wait();
                }

                if (!isConnected()){
                    //handshake failed, throw exception
                    throw new IDSCPv2Exception("Handshake failed");
                }

            } catch (InterruptedException e) {
                throw new IDSCPv2Exception("Handshake failed because thread was interrupted");
            }
        }
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

    private void reset(){
        this.currentState = initialState;
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

    private void notifyHandshakeCompleteLock(){
        synchronized (idscpHandshakeLock){
            idscpHandshakeLock.notify();
        }
    }
}
