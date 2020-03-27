package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FSM implements FsmListener{
    private static final Logger LOG = LoggerFactory.getLogger(FSM.class);

    /*  -----------   IDSCPv2 Protocol States   ---------- */
    private HashMap<FSM_STATE, State> states = new HashMap<>();

    enum FSM_STATE {
        STATE_CLOSED,
        STATE_WAIT_FOR_HELLO,
        STATE_WAIT_FOR_RAT,
        STATE_WAIT_FOR_RAT_VERIFIER,
        STATE_WAIT_FOR_RAT_PROVER,
        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER,
        STATE_WAIT_FOR_DAT_AND_RAT,
        STATE_ESTABLISHED
    }

    private State currentState;
    /*  ----------------   end of states   --------------- */

    private SecureChannel secureChannel;

    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;
    private String currentRatProverId; //avoid messages from old provers
    private String currentRatVerifierId; //avoid messages from old verifiers
    private String proverMechanism = null;
    private String verifierMechanism = null;

    private IdscpMsgListener listener = null;
    private final CountDownLatch listenerLatch = new CountDownLatch(1);
    private final ReentrantLock fsmIsBusy = new ReentrantLock(true); //use a fair synchronization lock (FIFO)
    private final Condition onMessageBlock = fsmIsBusy.newCondition();
    private final Condition idscpHandshakeLock = fsmIsBusy.newCondition();
    private boolean handshakeResultAvailable = false;
    private boolean fsmIsClosed = false;

    private Timer datTimer;
    private Timer ratTimer;
    private Timer handshakeTimer;

    public FSM(SecureChannel secureChannel, DapsDriver dapsDriver,
               String[] localSupportedRatSuite, String[] localExpectedRatSuite, int ratTimeout){

        this.secureChannel = secureChannel;
        secureChannel.setFsm(this);


        /* ------------- Timeout Handler Routines ------------*/
        Runnable handshakeTimeoutHandler = () -> {
            LOG.debug("HANDSHAKE_TIMER_EXPIRED");
            onControlMessage(InternalControlMessage.TIMEOUT);
        };

        Runnable datTimeoutHandler = () -> {
            LOG.debug("DAT_TIMER_EXPIRED");
            onControlMessage(InternalControlMessage.DAT_TIMER_EXPIRED);
        };

        Runnable ratTimeoutHandler = () -> {
            LOG.debug("RAT_TIMER_EXPIRED");
            onControlMessage(InternalControlMessage.REPEAT_RAT);
        };

        this.handshakeTimer = new Timer(fsmIsBusy, handshakeTimeoutHandler);
        this.datTimer = new Timer(fsmIsBusy, datTimeoutHandler);
        this.ratTimer = new Timer(fsmIsBusy, ratTimeoutHandler);
        /* ------------- end timeout routines ------------- */

        states.put(FSM_STATE.STATE_CLOSED, new StateClosed(this, dapsDriver, onMessageBlock,
                localSupportedRatSuite, localExpectedRatSuite));
        states.put(FSM_STATE.STATE_WAIT_FOR_HELLO, new StateWaitForHello(this,
                handshakeTimer, datTimer, dapsDriver, localSupportedRatSuite, localExpectedRatSuite));
        states.put(FSM_STATE.STATE_WAIT_FOR_RAT, new StateWaitForRat(this, handshakeTimer, ratTimer,
                ratTimeout, dapsDriver));
        states.put(FSM_STATE.STATE_WAIT_FOR_RAT_PROVER, new StateWaitForRatProver(this, ratTimer, handshakeTimer,
                dapsDriver));
        states.put(FSM_STATE.STATE_WAIT_FOR_RAT_VERIFIER, new StateWaitForRatVerifier(this, dapsDriver, ratTimer,
                handshakeTimer, ratTimeout));
        states.put(FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT, new StateWaitForDatAndRat(this, handshakeTimer,
                datTimer, dapsDriver));
        states.put(FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER, new StateWaitForDatAndRatVerifier(this,
                handshakeTimer, datTimer, dapsDriver));
        states.put(FSM_STATE.STATE_ESTABLISHED, new StateEstablished(this, dapsDriver, ratTimer, handshakeTimer));


        //set initial state
        currentState = states.get(FSM_STATE.STATE_CLOSED);
    }

    @Override
    public void onMessage(byte[] data){
        //parse message and create new IDSCP Message Event, then pass it to current state and update new state
        IdscpMessage message;
        try {
            message = IdscpMessage.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}", data);
            return;
        }

        Event event = new Event(message);
        //must wait when fsm is in state STATE_CLOSED --> wait() will be notified when fsm is leaving STATE_CLOSED
        fsmIsBusy.lock();
        try {
            while (currentState.equals(states.get(FSM_STATE.STATE_CLOSED))){
                try {
                    onMessageBlock.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            feedEvent(event);
        } finally {
            fsmIsBusy.unlock();
        }

    }

    @Override
    public void onControlMessage(InternalControlMessage controlMessage) {
        //create Internal Control Message Event and pass it to current state and update new state
        Event e = new Event(controlMessage);

        fsmIsBusy.lock();
        try {
            feedEvent(e);
        } finally {
            fsmIsBusy.unlock();
        }
    }

    @Override
    public void onRatProverMessage(InternalControlMessage controlMessage, IdscpMessage idscpMessage) {
        //only allow rat prover messages from current thread
        Event e;
        if (idscpMessage == null){
            e = new Event(controlMessage);
        } else {
            e = new Event(controlMessage, idscpMessage);
        }

        fsmIsBusy.lock();
        try {
            if (Long.toString(Thread.currentThread().getId()).equals(currentRatProverId)) {
                feedEvent(e);
            } else {
                LOG.warn("An old or unknown identity calls onRatProverMessage()");
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    @Override
    public void onRatVerifierMessage(InternalControlMessage controlMessage, IdscpMessage idscpMessage) {
        //only allow rat verifier messages from current thread
        Event e;
        if (idscpMessage == null){
            e = new Event(controlMessage);
        } else {
            e = new Event(controlMessage, idscpMessage);
        }

        fsmIsBusy.lock();
        try{
            if (Long.toString(Thread.currentThread().getId()).equals(currentRatVerifierId)) {
                feedEvent(e);
            } else {
                LOG.warn("An old or unknown identity calls onRatVerifierMessage()");
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    private void feedEvent(Event event){
        State prevState = currentState;
        currentState = currentState.feedEvent(event);

        if (!prevState.equals(currentState)) {
            currentState.runEntryCode(this);
        }
    }

    public void terminate(){
        LOG.info("Close idscp connection");
        onControlMessage(InternalControlMessage.IDSCP_STOP);
        LOG.debug("Close secure channel");
        secureChannel.close();
    }

    public void startIdscpHandshake() throws IDSCPv2Exception {
        fsmIsBusy.lock();
        try {
            if (currentState.equals(states.get(FSM_STATE.STATE_CLOSED))) {
                if (fsmIsClosed) {
                    throw new IDSCPv2Exception("FSM is in a final closed state forever");
                }

                //trigger handshake init
                onControlMessage(InternalControlMessage.START_IDSCP_HANDSHAKE);

                //wait until handshake was successful or failed
                while (!handshakeResultAvailable) {
                    idscpHandshakeLock.await();
                }

                if (!isConnected()){
                    //handshake failed, throw exception
                    throw new IDSCPv2Exception("Handshake failed");
                }

            } else {
                throw new IDSCPv2Exception("Handshake has already been started");
            }
        } catch (InterruptedException e) {
            throw new IDSCPv2Exception("Handshake failed because thread was interrupted");
        } finally {
            fsmIsBusy.unlock();
        }
    }

    void sendFromFSM(IdscpMessage msg){
        //send messages from fsm
        secureChannel.send(msg.toByteArray());
    }

    @Override
    public void onError(){
        onControlMessage(InternalControlMessage.ERROR);
    }

    @Override
    public void onClose(){
        onControlMessage(InternalControlMessage.ERROR);
    }

    public void send(IdscpMessage msg){
        //send messages from user only when idscp connection is established
        fsmIsBusy.lock();
        try{
            if(isConnected()){
                secureChannel.send(msg.toByteArray());
            } else {
                LOG.error("Cannot send IDSCP_DATA because protocol is not established");
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    public boolean isConnected(){
        return currentState.equals(states.get(FSM_STATE.STATE_ESTABLISHED));
    }

    public void registerMessageListener(IdscpMsgListener listener){
        this.listener = listener;
        listenerLatch.countDown();
    }

    void notifyHandshakeCompleteLock(){
        fsmIsBusy.lock();
        try {
            handshakeResultAvailable = true;
            idscpHandshakeLock.signal();
        } finally {
            fsmIsBusy.unlock();
        }
    }

    //calculate Prover mechanism (strongest remote expected), returns null if no match was found
    String getRatProverMechanism(String[] localSupportedProver, Object[] remoteExpectedVerifier){
        //toDo implement logic
        return localSupportedProver[0];
    }

    //calculate Verifier mechanism (strongest local expected), returns null if no match was found
    String getRatVerifierMechanism(String[] localExpectedVerifier, Object[] remoteSupportedProver){
        //toDo implement logic
        return localExpectedVerifier[0];
    }

    void restartRatVerifierDriver(){
        //assume verifier mechanism is set
        stopRatVerifierDriver();
        ratVerifierDriver = RatVerifierDriverRegistry.startRatVerifierDriver(verifierMechanism, this);
        if (ratVerifierDriver == null){
            LOG.error("Cannot create instance of RAT_VERIFIER_DRIVER");
            currentRatVerifierId = "";
            onControlMessage(InternalControlMessage.ERROR); //toDo geht das? nein
        } else {
            currentRatVerifierId = Long.toString(ratVerifierDriver.getId());
        }
    }

    void stopRatVerifierDriver(){
        if (ratVerifierDriver != null && ratVerifierDriver.isAlive()){
            ratVerifierDriver.interrupt();
            ratVerifierDriver.terminate();
        }
    }

    void restartRatProverDriver(){
        //assume prover mechanism is set
        stopRatProverDriver();
        ratProverDriver = RatProverDriverRegistry.startRatProverDriver(proverMechanism, this);
        if (ratProverDriver == null){
            LOG.error("Cannot create instance of RAT_PROVER_DRIVER");
            currentRatProverId = "";
            onControlMessage(InternalControlMessage.ERROR); //toDo geht das? nein
        } else {
            currentRatProverId = Long.toString(ratProverDriver.getId());
        }
    }

    void stopRatProverDriver(){
        if (ratProverDriver != null && ratProverDriver.isAlive()){
            ratProverDriver.interrupt();
            ratProverDriver.terminate();
        }
    }

    void setRatMechanisms(String proverMechanism, String verifierMechanism) {
        this.proverMechanism = proverMechanism;
        this.verifierMechanism = verifierMechanism;
    }



    State getState(FSM_STATE state){
        return states.get(state);
    }

    void lockFsm(){
        try {
            secureChannel.close();
            this.datTimer.cancelTimeout();
            this.datTimer = null;
            this.ratTimer.cancelTimeout();
            this.ratTimer = null;
            this.handshakeTimer.cancelTimeout();
            this.handshakeTimer = null;
            this.stopRatProverDriver();
            this.stopRatVerifierDriver();
            fsmIsClosed = true;
        } catch (NullPointerException ignored){}

        //inform upper layer via handshake or closeListener
        try {
            if (handshakeResultAvailable){
                listenerLatch.await();
                listener.onClose();
            } else {
                notifyHandshakeCompleteLock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    RatVerifierDriver getRatVerifierDriver() {
        return ratVerifierDriver;
    }

    RatProverDriver getRatProverDriver() {
        return ratProverDriver;
    }

    void notifyIdscpMsgListener(byte[] data) {
        try {
            this.listenerLatch.await();
            this.listener.onMessage(data);
            LOG.debug("Idscp data were passed to connection listener");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
