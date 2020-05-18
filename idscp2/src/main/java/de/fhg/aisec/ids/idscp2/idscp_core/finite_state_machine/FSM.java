package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver;
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection;
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCP2.IdscpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The finite state machine FSM of the IDSCP2 protocol
 *
 * Manages IDSCP2 Handshake, Re-Attestation, DAT-ReRequest and DAT-Re-Validation. Delivers
 * Internal Control Messages and Idscpv2Messages to the target receivers,
 * creates and manages the states and its transitions and implements security restriction to protect
 * the protocol against misuse and faulty, insecure or evil driver implementations.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class FSM implements FsmListener{
    private static final Logger LOG = LoggerFactory.getLogger(FSM.class);

    /*  -----------   IDSCP2 Protocol States   ---------- */
    private final HashMap<FSM_STATE, State> states = new HashMap<>();

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

    private final SecureChannel secureChannel; //secure underlying channel

    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;

    /*
     * RAT Driver Thread ID to identify the driver threads and check if messages are provided by
     * the current active driver or by any old driver, whose lifetime is already over
     *
     * Only one driver can be valid at a time
     */
    private String currentRatProverId; //avoid messages from old prover drivers
    private String currentRatVerifierId; //avoid messages from old verifier drivers

    /*
     * RAT Mechanisms, calculated during handshake in WAIT_FOR_HELLO_STATE
     */
    private String proverMechanism = null; //RAT prover mechanism
    private String verifierMechanism = null; //RAT Verifier mechanism

    /*
     * The idscp message listener, that should receive the IDSCP2 messages from the fsm
     * And a listener latch to ensure that the listener is available and the message does not get
     * lost
     */
    private Idscp2Connection connection;
    private final CountDownLatch connectionLatch = new CountDownLatch(1);

    /*
     * A FIFO-fair synchronization lock for the finite state machine
     */
    private final ReentrantLock fsmIsBusy = new ReentrantLock(true);

    /*
     * A condition to ensure no idscp messages can be provided by the secure channel to the fsm
     * before the handshake was started
     */
    private final Condition onMessageBlock = fsmIsBusy.newCondition();

    /*
     * A condition for the dscpv2 handshake to wait until the handshake was successful and the
     * connection is established or the handshake failed and the fsm is locked forever
     */
    private final Condition idscpHandshakeLock = fsmIsBusy.newCondition();
    private boolean handshakeResultAvailable = false;

    /*
     * Check if FSM is closed forever
     */
    private boolean fsmIsClosed = false;

    /* ---------------------- Timer ---------------------- */
    private final Timer datTimer;
    private final Timer ratTimer;
    private final Timer handshakeTimer;
    private final Timer proverHandshakeTimer;
    private final Timer verifierHandshakeTimer;
    /*  ----------------   end of Timer   --------------- */

    public FSM(SecureChannel secureChannel, DapsDriver dapsDriver,
               String[] localSupportedRatSuite, String[] localExpectedRatSuite, int ratTimeout){


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

        Runnable proverTimeoutHandler = () -> {
            LOG.debug("RAT_PROVER_HANDSHAKE_TIMER_EXPIRED");
            onControlMessage(InternalControlMessage.REPEAT_RAT);
        };

        Runnable verifierTimeoutHandler = () -> {
            LOG.debug("RAT_VERIFIER_HANDSHAKE_TIMER_EXPIRED");
            onControlMessage(InternalControlMessage.REPEAT_RAT);
        };

        this.handshakeTimer = new Timer(fsmIsBusy, handshakeTimeoutHandler);
        this.datTimer = new Timer(fsmIsBusy, datTimeoutHandler);
        this.ratTimer = new Timer(fsmIsBusy, ratTimeoutHandler);
        this.proverHandshakeTimer = new Timer(fsmIsBusy, proverTimeoutHandler);
        this.verifierHandshakeTimer = new Timer(fsmIsBusy, verifierTimeoutHandler);
        /* ------------- end timeout routines ------------- */

        /* ------------- FSM STATE Initialization -------------*/
        states.put(FSM_STATE.STATE_CLOSED, new StateClosed(this, dapsDriver, onMessageBlock,
                localSupportedRatSuite, localExpectedRatSuite));

        states.put(FSM_STATE.STATE_WAIT_FOR_HELLO, new StateWaitForHello(this,
                handshakeTimer, datTimer, dapsDriver, localSupportedRatSuite, localExpectedRatSuite
        ));

        states.put(FSM_STATE.STATE_WAIT_FOR_RAT, new StateWaitForRat(this, handshakeTimer,
            verifierHandshakeTimer, proverHandshakeTimer, ratTimer, ratTimeout, dapsDriver));

        states.put(FSM_STATE.STATE_WAIT_FOR_RAT_PROVER, new StateWaitForRatProver(this,
            ratTimer, handshakeTimer, proverHandshakeTimer, dapsDriver));

        states.put(FSM_STATE.STATE_WAIT_FOR_RAT_VERIFIER, new StateWaitForRatVerifier(this,
            dapsDriver, ratTimer, handshakeTimer, verifierHandshakeTimer, ratTimeout));

        states.put(FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT, new StateWaitForDatAndRat(this,
            handshakeTimer, proverHandshakeTimer, datTimer, dapsDriver));

        states.put(FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER,
            new StateWaitForDatAndRatVerifier(this, handshakeTimer, datTimer, dapsDriver));

        states.put(FSM_STATE.STATE_ESTABLISHED, new StateEstablished(this, dapsDriver,
            ratTimer, handshakeTimer));


        //set initial state
        currentState = states.get(FSM_STATE.STATE_CLOSED);

        //register fsm to the secure channel for bi-directional communication between fsm and sc
        this.secureChannel = secureChannel;
        secureChannel.setFsm(this);
    }

    private void checkForFsmCircles() {

        // check if current thread holds already the fsm lock, then we have a circle
        // this runs into an issue: onControlMessage must be called only from other threads!
        // if the current thread currently stuck within a fsm transition it will trigger another
        // transition on the old state and undefined behaviour occurred
        //
        // The idscpv2 core and default driver will not run into this issue. It's a protection for
        // avoiding incorrect usage of the idscpv2 library from further driver implementations
        //
        // Example:
        // Thread A stuck within a transition t1 that calls a function that calls
        // onControlMessage(InternalError). Then the error is handled in the current
        // state and the fsm will switch into state STATE_CLOSED and close all resources.
        //
        // Afterwards, the thread will continue the old transition t1 that might use some of the
        // closed resources and switch in a non-closed STATE, e.g. STATE_ESTABLISHED.
        // So our fsm would be broken and the behaviour is undefined and could leak security
        // vulnerabilities
        //
        if (fsmIsBusy.isHeldByCurrentThread()) {
            throw new RuntimeException("The current thread holds the fsm lock already. "
                + "A circle might occur that could lead to undefined behaviour within the fsm");
        }
    }

    /*
     * Get a new IDSCP2 Message from the secure channel and provide it as an event to the fsm
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     */
    @Override
    public void onMessage(byte[] data){

        //check for incorrect usage
        checkForFsmCircles();

        //parse message and create new IDSCP Message event, then pass it to current state and
        // update new state
        IdscpMessage message;
        try {
            message = IdscpMessage.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}", data);
            return;
        }

        Event event = new Event(message);
        //must wait when fsm is in state STATE_CLOSED --> wait() will be notified when fsm is
        // leaving STATE_CLOSED
        fsmIsBusy.lock();
        try {
            while (currentState.equals(states.get(FSM_STATE.STATE_CLOSED))){

                if (fsmIsClosed) {
                    return;
                }

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

    /*
     * An internal control message (ICM) occurred, provide it to the fsm as an event
     */
    private void onControlMessage(InternalControlMessage controlMessage) {
        //create Internal Control Message Event and pass it to current state and update new state
        Event e = new Event(controlMessage);

        fsmIsBusy.lock();
        try {
            feedEvent(e);
        } finally {
            fsmIsBusy.unlock();
        }
    }

    /*
     * API for RatProver to provide Prover Messages to the fsm
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     *
     * Afterwards the event for the fsm is created and the fsm lock is requested
     *
     * When the RatProverThread does not match the active prover tread id, the event will be
     * ignored, else the event is provided to the fsm
     */
    @Override
    public void onRatProverMessage(InternalControlMessage controlMessage, byte[] ratMessage) {

        //check for incorrect usage
        checkForFsmCircles();

        //only allow rat prover messages from current thread
        Event e;
        if (ratMessage == null){
            e = new Event(controlMessage);
        } else {
            IdscpMessage idscpMessage = Idscp2MessageHelper.createIdscpRatProverMessage(ratMessage);
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

    /*
     * API for RatVerifier to provide Verifier Messages to the fsm
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     *
     * Afterwards the event for the fsm is created and the fsm lock is requested
     *
     * When the RatVerifierDriver does not match the active verifier tread id, the event will be
     * ignored, else the event is provided to the fsm
     */
    @Override
    public void onRatVerifierMessage(InternalControlMessage controlMessage, byte[] ratMessage) {

        //check for incorrect usage
        checkForFsmCircles();

        //only allow rat verifier messages from current thread
        Event e;
        if (ratMessage == null){
            e = new Event(controlMessage);
        } else {
            IdscpMessage idscpMessage = Idscp2MessageHelper.createIdscpRatVerifierMessage(ratMessage);
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

    /*
     * Feed the event to the current state and execute the runEntry method if the state has changed
     */
    private void feedEvent(Event event){
        State prevState = currentState;
        currentState = currentState.feedEvent(event);

        if (!prevState.equals(currentState)) {
            currentState.runEntryCode(this);
        }
    }

    /*
     * API to terminate the idscp connection by the user
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     */
    public void terminate(){

        //check for incorrect usage
        checkForFsmCircles();

        LOG.info("Close idscp connection");
        onControlMessage(InternalControlMessage.IDSCP_STOP);
        LOG.debug("Close secure channel");
        secureChannel.close();
    }

    /*
     * API for the user to start the IDSCP2 handshake
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     */
    public void startIdscpHandshake() throws Idscp2Exception {

        //check for incorrect usage
        checkForFsmCircles();

        fsmIsBusy.lock();
        try {
            if (currentState.equals(states.get(FSM_STATE.STATE_CLOSED))) {
                if (fsmIsClosed) {
                    throw new Idscp2Exception("FSM is in a final closed state forever");
                }

                //trigger handshake init
                onControlMessage(InternalControlMessage.START_IDSCP_HANDSHAKE);

                //wait until handshake was successful or failed
                while (!handshakeResultAvailable) {
                    idscpHandshakeLock.await();
                }

                if (!isConnected()){
                    //handshake failed, throw exception
                    throw new Idscp2Exception("Handshake failed");
                }

            } else {
                throw new Idscp2Exception("Handshake has already been started");
            }
        } catch (InterruptedException e) {
            throw new Idscp2Exception("Handshake failed because thread was interrupted");
        } finally {
            fsmIsBusy.unlock();
        }
    }

    /*
     * Send idscp data from the fsm via the secure channel to the peer
     */
    boolean sendFromFSM(IdscpMessage msg){
        //send messages from fsm
        return secureChannel.send(msg.toByteArray());
    }

    /*
     * Provide an Internal Control Message to the FSM
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     */
    @Override
    public void onError(){

        //check for incorrect usage
        checkForFsmCircles();

        onControlMessage(InternalControlMessage.ERROR);
    }

    /*
     * Provide an Internal Control Message to the FSM, caused by a secure channel closure
     *
     * The checkForFsmCircles method first checks for risky thread circles that occur by incorrect
     * driver implementations
     */
    @Override
    public void onClose(){

        //check for incorrect usage
        checkForFsmCircles();

        onControlMessage(InternalControlMessage.ERROR);
    }

    /*
     * Send idscp message from the User via the secure channel
     */
    public void send(String type, byte[] msg){
        //send messages from user only when idscp connection is established
        fsmIsBusy.lock();
        try{
            if(isConnected()){
                IdscpMessage idscpMessage = Idscp2MessageHelper.createIdscpDataMessage(type, msg);
                if (!secureChannel.send(idscpMessage.toByteArray())) {
                    LOG.error("Cannot send IDSCP_DATA via secure channel");
                    onControlMessage(InternalControlMessage.ERROR);
                }
            } else {
                LOG.error("Cannot send IDSCP_DATA because connection is not established");
            }
        } finally {
            fsmIsBusy.unlock();
        }
    }

    /*
     * Check if FSM is in STATE ESTABLISHED
     */
    public boolean isConnected(){
        return currentState.equals(states.get(FSM_STATE.STATE_ESTABLISHED));
    }

    /*
     * Register an IDSCP2 message listener
     */
    public void registerConnection(Idscp2Connection connection){
        this.connection = connection;
        connectionLatch.countDown();
    }

    /*
     * Notify handshake lock about result
     */
    void notifyHandshakeCompleteLock(){
        fsmIsBusy.lock();
        try {
            handshakeResultAvailable = true;
            idscpHandshakeLock.signal();
        } finally {
            fsmIsBusy.unlock();
        }
    }

    /*
     * Calculate the RatProver mechanism
     *
     * Return the String of the cipher or null if no match was found
     */
    String getRatProverMechanism(String[] localSupportedProver, Object[] remoteExpectedVerifier){
        //toDo implement logic
        return localSupportedProver[0];
    }

    /*
     * Calculate the RatVerifier mechanism
     *
     * Return the String of the cipher or null if no match was found
     */
    String getRatVerifierMechanism(String[] localExpectedVerifier, Object[] remoteSupportedProver){
        //toDo implement logic
        return localExpectedVerifier[0];
    }

    /*
     * Stop current RatVerifier if active and start the RatVerifier from the
     * RatVerifierDriver Registry that matches the verifier mechanism
     *
     * return false if no match was found
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean restartRatVerifierDriver(){
        //assume verifier mechanism is set
        stopRatVerifierDriver();
        ratVerifierDriver = RatVerifierDriverRegistry.startRatVerifierDriver(verifierMechanism, this);
        if (ratVerifierDriver == null){
            LOG.error("Cannot create instance of RAT_VERIFIER_DRIVER");
            currentRatVerifierId = "";
            return false;
        } else {
            //safe the thread ID
            currentRatVerifierId = Long.toString(ratVerifierDriver.getId());
            LOG.debug("Start verifier_handshake timeout");
            this.verifierHandshakeTimer.resetTimeout(5);
            return true;
        }
    }

    /*
     * Terminate the RatVerifierDriver
     */
    void stopRatVerifierDriver() {
        verifierHandshakeTimer.cancelTimeout();
        if (ratVerifierDriver != null && ratVerifierDriver.isAlive()){
            ratVerifierDriver.interrupt();
            ratVerifierDriver.terminate();
        }
    }

    /*
     * Stop current RatProver if active and start the RatProver from the
     * RatProverDriver Registry that matches the prover mechanism
     *
     * return false if no match was found
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean restartRatProverDriver(){
        //assume prover mechanism is set
        stopRatProverDriver();
        ratProverDriver = RatProverDriverRegistry.startRatProverDriver(proverMechanism, this);
        if (ratProverDriver == null){
            LOG.error("Cannot create instance of RAT_PROVER_DRIVER");
            currentRatProverId = "";
            return false;
        } else {
            //safe the thread ID
            currentRatProverId = Long.toString(ratProverDriver.getId());
            LOG.debug("Start prover_handshake timeout");
            this.proverHandshakeTimer.resetTimeout(5);
            return true;
        }
    }

    /*
     * Terminate the RatProverDriver
     */
    void stopRatProverDriver(){
        proverHandshakeTimer.cancelTimeout();
        if (ratProverDriver != null && ratProverDriver.isAlive()){
            ratProverDriver.interrupt();
            ratProverDriver.terminate();
        }
    }

    /*
     * Lock the fsm forever, terminate the timers and drivers, close the secure channel
     * and notify User or handshake lock about closure
     */
    void shutdownFsm() {
        secureChannel.close();
        datTimer.cancelTimeout();
        ratTimer.cancelTimeout();
        handshakeTimer.cancelTimeout();
        // Cancels proverHandshakeTimer
        this.stopRatProverDriver();
        // Cancels verifierHandshakeTimer
        this.stopRatVerifierDriver();
        fsmIsClosed = true;

        //notify upper layer via handshake or closeListener
        try {
            if (handshakeResultAvailable) {
                connectionLatch.await();
                connection.onClose();
            } else {
                notifyHandshakeCompleteLock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Provide IDSCP2 message to the message listener
     */
    void notifyIdscpMsgListener(String type, byte[] data) {
        try {
            this.connectionLatch.await();
            this.connection.onMessage(type, data);
            LOG.debug("Idscp data were passed to connection listener");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //
    // Getter
    //

    RatVerifierDriver getRatVerifierDriver() {
        return ratVerifierDriver;
    }

    RatProverDriver getRatProverDriver() {
        return ratProverDriver;
    }

    State getState(FSM_STATE state){
        return states.get(state);
    }

    //
    // Setter
    //

    void setRatMechanisms(String proverMechanism, String verifierMechanism) {
        this.proverMechanism = proverMechanism;
        this.verifierMechanism = verifierMechanism;
    }
}
