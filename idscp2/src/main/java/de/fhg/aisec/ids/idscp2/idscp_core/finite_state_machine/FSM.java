package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import com.google.protobuf.InvalidProtocolBufferException;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.*;
import de.fhg.aisec.ids.idscp2.error.IDSCPv2Exception;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMsgListener;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry;
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class FSM implements FsmListener{
    private static final Logger LOG = LoggerFactory.getLogger(FSM.class);

    /*  -----------   IDSCPv2 Protocol States   ---------- */
    private final State STATE_CLOSED = new State();
    private final State STATE_WAIT_FOR_HELLO = new State();
    private final State STATE_WAIT_FOR_RAT= new State();
    private final State STATE_WAIT_FOR_RAT_VERIFIER = new State();
    private final State STATE_WAIT_FOR_RAT_PROVER = new State();
    private final State STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER = new State();
    private final State STATE_WAIT_FOR_DAT_AND_RAT = new State();
    private final State STATE_ESTABLISHED = new State();

    private State currentState;
    private final State initialState = STATE_CLOSED;
    /*  ----------------   end of states   --------------- */

    private SecureChannel secureChannel;
    private DapsDriver dapsDriver;

    private RatProverDriver ratProverDriver;
    private RatVerifierDriver ratVerifierDriver;
    private String currentRatProverId; //avoid messages from old provers
    private String currentRatVerifierId; //avoid messages from old verifiers
    private String proverMechanism = null;
    private String verifierMechanism = null;

    private IdscpMsgListener listener = null;
    private final CountDownLatch listenerLatch = new CountDownLatch(1);
    private final Object idscpHandshakeLock = new Object();
    private final Object fsmIsBusy = new Object();

    //toDo cipher suites
    private String[] localSupportedRatSuite = new String[] {"TPM_2, TRUST_ZONE, SGX"};
    private String[] localExpectedRatSuite = new String[] {"TPM_2, TRUST_ZONE, SGX"};
    private byte[] remoteDat = null;

    private Timer datTimer;
    private Timer ratTimer;
    private Timer handshakeTimer;
    private int ratTimeoutDelay = 3600;

    public FSM(SecureChannel secureChannel, DapsDriver dapsDriver){

        this.secureChannel = secureChannel;
        this.dapsDriver = dapsDriver;
        secureChannel.setFsm(this);

        /* ------------- Timeout Handler Routines ------------*/
        Runnable handshakeTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                //onControlMessage(InternalControlMessage.TIMEOUT);
                System.out.println("HANDSHAKE_TIMER_EXPIRED");

            }
        };

        Runnable datTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                //onControlMessage(InternalControlMessage.DAT_TIMER_EXPIRED);
                System.out.println("DAT_TIMER_EXPIRED");
            }
        };

        Runnable ratTimeoutHandler = new Runnable() {
            @Override
            public void run() {
                //onControlMessage(InternalControlMessage.RAT_TIMER_EXPIRED);
                System.out.println("RAT_TIMER_EXPIRED");
            }
        };
        //toDo set correct delays
        this.handshakeTimer = new Timer(fsmIsBusy, handshakeTimeoutHandler);
        this.datTimer = new Timer(fsmIsBusy, datTimeoutHandler);
        this.ratTimer = new Timer(fsmIsBusy, ratTimeoutHandler);
        /* ------------- end timeout routines ------------- */


        /*  -----------   Protocol Transitions   ---------- */

        /*---------------------------------------------------
        * STATE_CLOSED - Transition Description
        * ---------------------------------------------------
        * onICM: start_handshake --> {send IDSCP_HELLO, set handshake_timeout} --> STATE_WAIT_FOR_HELLO
        * ALL_OTHER_MESSAGES ---> STATE_CLOSED
        * --------------------------------------------------- */
        STATE_CLOSED.addTransition(InternalControlMessage.START_IDSCP_HANDSHAKE.getValue(), new Transition(
                event -> {
                    LOG.debug("Get DAT Token vom DAT_DRIVER");
                    byte[] dat = this.dapsDriver.getToken();
                    LOG.debug("Send IDSCP_HELLO");
                    IdscpMessage idscpHello = IdscpMessageFactory.
                            getIdscpHelloMessage(dat, this.localSupportedRatSuite, this.localExpectedRatSuite);
                    sendFromFSM(idscpHello);
                    fsmIsBusy.notify(); //enables fsm.onMessage()
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_HELLO");
                    return STATE_WAIT_FOR_HELLO;
                }
        ));

        STATE_CLOSED.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
            );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_HELLO - Transition Description
         * ---------------------------------------------------
         * onICM: error --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: stop --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: timeout --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_CLOSE---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (no rat match) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (invalid DAT) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (SUCCESS) ---> {verify DAT, match RAT, set DAT Timeout, start RAT P&V,
         *                                        set handshake_timeout} ---> STATE_WAIT_FOR_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_HELLO
         * --------------------------------------------------- */
        STATE_WAIT_FOR_HELLO.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_HELLO.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_HELLO.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("STATE_WAIT_FOR_HELLO timeout. Send IDSCP_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake Timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_HELLO.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Received IDSCP_CLOSE");
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_HELLO.addTransition(IdscpMessage.IDSCPHELLO_FIELD_NUMBER, new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Received IDSCP_HELLO");
                    IdscpHello idscpHello = event.getIdscpMessage().getIdscpHello();

                    LOG.debug("Calculate Rat mechanisms");
                    this.proverMechanism = getRatProverMechanism(this.localSupportedRatSuite,
                            idscpHello.getExpectedRatSuiteList().toArray());
                    if (proverMechanism == null){
                        LOG.debug("Cannot find a match for RAT proverr. Send IDSCP_CLOSE");
                        sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No match for RAT Prover mechanism",
                                IdscpClose.CloseCause.NO_RAT_MECHANISM_MATCH_PROVER));
                        LOG.debug("Switch to state STATE_CLOSED");
                        notifyHandshakeCompleteLock();
                        return STATE_CLOSED;
                    }

                    this.verifierMechanism = getRatVerifierMechanism(localExpectedRatSuite,
                            idscpHello.getSupportedRatSuiteList().toArray());
                    if (verifierMechanism == null){
                        LOG.debug("Cannot find a match for RAT verifier. Send IDSCP_CLOSE");
                        sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No match for RAT Verifier mechanism",
                                IdscpClose.CloseCause.NO_RAT_MECHANISM_MATCH_VERIFIER));
                        LOG.debug("Switch to state STATE_CLOSED");
                        notifyHandshakeCompleteLock();
                        return STATE_CLOSED;
                    }

                    LOG.debug("Verify received DAT");
                    //check if Dat is available and verify dat
                    byte[] dat;
                    int datValidityPeriod;
                    if (!idscpHello.hasDynamicAttributeToken() || 0 > (datValidityPeriod= this.dapsDriver
                            .verifyToken(dat = idscpHello.getDynamicAttributeToken().getToken().toByteArray()))){
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No valid DAT", IdscpClose.CloseCause.NO_VALID_DAT));
                        LOG.debug("Switch to state STATE_CLOSED");
                        notifyHandshakeCompleteLock();
                        return STATE_CLOSED;
                    }
                    this.remoteDat = dat;
                    LOG.debug("Remote DAT is valid. Set dat timeout to its validity period");
                    datTimer.resetTimeout(datValidityPeriod);

                    LOG.debug("Start RAT Prover and Verifier");
                    restartRatVerifierDriver();
                    restartRatProverDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT");
                    notifyHandshakeCompleteLock();
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_HELLO.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_HELLO");
                    return STATE_WAIT_FOR_HELLO;
                }
        );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {ratP.stop(), ratV.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> IDSCP_CLOSED
         * onICM: stop ---> {ratP.stop(), ratV.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> IDSCP_CLOSED
         * onICM: rat_prover_ok ---> {} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onICM: rat_verifier_ok ---> {set rat timeout} ---> STATE_WAIT_FOR_RAT_PROVER
         * onICM: rat_prover_failed ---> {ratV.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_verifier_failed ---> {ratP.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onICM: rat_verifier_msg ---> {send IDSCP_RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onICM: dat_timeout ---> {send DAT_EXPIRED, ratV.cancel()} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onICM: handshake_timeout ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_RAT_PROVER ---> {delegate to RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_DAT_EXPIRED ---> {send DAT, ratP.restart()} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {ratP.stop(), ratV.stop(), timeouts.stop()} ---> IDSCP_CLOSED
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT
         * --------------------------------------------------- */
        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event ->  {
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    stopRatProverDriver();
                    stopRatVerifierDriver();
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    stopRatProverDriver();
                    stopRatVerifierDriver();
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_PROVER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_PROVER OK");
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_VERIFIER");
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_VERIFIER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_VERIFIER OK");
                    ratTimer.resetTimeout(this.ratTimeoutDelay);
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_PROVER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_PROVER failed");
                    LOG.debug("Send IDSC_CLOSE");
                    stopRatVerifierDriver();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_PROVER failed", IdscpClose.CloseCause.RAT_PROVER_FAILED));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_VERIFIER failed");
                    stopRatProverDriver();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_VERIFIER failed", IdscpClose.CloseCause.RAT_VERIFIER_FAILED));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_PROVER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_PROVER");
                    sendFromFSM(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.RAT_VERIFIER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_VERIFIER");
                    sendFromFSM(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    LOG.debug("DAT timeout, send IDSCP_DAT_EXPIRED and cancel RAT_VERIFIER");
                    stopRatVerifierDriver();
                    sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage());
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    stopRatProverDriver();
                    stopRatVerifierDriver();
                    this.datTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER");
                    ratProverDriver.delegate(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER");
                    ratVerifierDriver.delegate(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    stopRatVerifierDriver();
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return STATE_WAIT_FOR_RAT;
                }
        );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {send IDSCP_CLOSE, stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: close ---> {send IDSCP_CLOSE, stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED, cancel ratV} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: rat_verifier_ok ---> {set rat timeout} ---> STATE_ESTABLISHED
         * onICM: rat_verifier_failed ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_verifier_msg ---> {send IDSCP_RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {stop RAT_VERIFIER} ---> STATE_CLOSED
         * onMessage: IDSCP_RAT_PROVER ---> {delegat to RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onMessage: IDSCP_RE_RAT ---> {start RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * --------------------------------------------------- */
        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    stopRatVerifierDriver();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    this.datTimer.cancelTimeout();
                    stopRatVerifierDriver();
                    this.handshakeTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    stopRatVerifierDriver();
                    datTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    LOG.debug("DAT timeout occurred. Send IDSCP_DAT_EXPIRED and stop RAT_VERIFIER");
                    stopRatVerifierDriver();
                    sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage());
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER");
                    notifyHandshakeCompleteLock();
                    return STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.RAT_VERIFIER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_VERIFIER OK");
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Start RAT Timer");
                    this.ratTimer.resetTimeout(ratTimeoutDelay);
                    LOG.debug("Switch to state STATE_ESTABLISHED");
                    notifyHandshakeCompleteLock();
                    return STATE_ESTABLISHED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_VERIFIER failed");
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_VERIFIER failed", IdscpClose.CloseCause.RAT_VERIFIER_FAILED));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(InternalControlMessage.RAT_VERIFIER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_VERIFIER");
                    sendFromFSM(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_VERIFIER");
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    stopRatVerifierDriver();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER");
                    ratVerifierDriver.delegate(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_VERIFIER");
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER");
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT_VERIFIER.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_VERIFIER");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT_PROVER - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE, stop RAT_PROVER, timeouts.terminate()} ---> STATE_CLOSED
         * onICM: error ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onICM: rat_prover_ok ---> {} ---> STATE_ESTABLISHED
         * onICM: rat_prover_failed ---> {send IDSCP_CLOSE, terminate ratP, cancel timeouts} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onICM: repeat_rat ---> {send IDSCP_RE_RAT, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, restart RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_RE_RAT ---> {restart RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT_PROVER
         * --------------------------------------------------- */
        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    stopRatProverDriver();
                    this.ratTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    stopRatProverDriver();
                    this.ratTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    this.handshakeTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    stopRatProverDriver();
                    this.datTimer.cancelTimeout();
                    this.ratTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    LOG.debug("DAT timeout occurred. Send IDSCP_DAT_EXPIRED");
                    this.ratTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage());
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT");
                    notifyHandshakeCompleteLock();
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.RAT_PROVER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_PROVER OK");
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_ESTABLISHED");
                    notifyHandshakeCompleteLock();
                    return STATE_ESTABLISHED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.RAT_PROVER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_PROVER failed");
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    this.ratTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_PROVER failed", IdscpClose.CloseCause.RAT_PROVER_FAILED));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.RAT_PROVER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_PROVER");
                    sendFromFSM(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(InternalControlMessage.REPEAT_RAT.getValue(), new Transition(
                event -> {
                    LOG.debug("Request RAT repeat. Send IDSCP_RE_RAT, start RAT_VERIFIER");
                    sendFromFSM(IdscpMessageFactory.getIdscpReRatMessage(""));
                    this.ratTimer.cancelTimeout();
                    restartRatVerifierDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    this.datTimer.cancelTimeout();
                    this.ratTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER");
                    ratProverDriver.delegate(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Restart RAT_PROVER");
                    restartRatProverDriver();
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_WAIT_FOR_RAT_PROVER.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: error ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_CLOSE ---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT(success) --> {verify DAT, set det_timeout, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFEIER
         * onMessage: IDSCP_DAT(failed) --> {verify DAT, send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RE_RAT ---> {start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * --------------------------------------------------- */
        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    this.handshakeTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPDAT_FIELD_NUMBER, new Transition(
                event -> {
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Verify received DAT");
                    //check if Dat is available and verify dat
                    byte[] dat = event.getIdscpMessage().getIdscpDat().getToken().toByteArray();
                    int datValidityPeriod;
                    if (0 > (datValidityPeriod = this.dapsDriver.verifyToken(dat))){
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No valid DAT", IdscpClose.CloseCause.NO_VALID_DAT));
                        LOG.debug("Switch to state STATE_CLOSED");
                        notifyHandshakeCompleteLock();
                        return STATE_CLOSED;
                    }
                    this.remoteDat = dat;
                    LOG.debug("Remote DAT is valid. Set dat timeout");
                    datTimer.resetTimeout(datValidityPeriod);
                    //start RAT Verifier
                    restartRatVerifierDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_VERIFIER");
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER");
                    restartRatProverDriver();
                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER");
                    return STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER;
                }
        );

        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: error ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: rat_prover_ok ---> {} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onICM: rat_prover_failed ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_CLOSE ---> {ratP.stop(), timeouts.stop()} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT(success) ---> {verify dat, start dat_timeout, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_DAT(failed) ---> {verify dat, send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, restart RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RE_RAT ---> {restart RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_ESTABLISHED
         * --------------------------------------------------- */
        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred, send IDSC_CLOSE");
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    stopRatProverDriver();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IdscpClose.CloseCause.TIMEOUT));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.RAT_PROVER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_PROVER OK");
                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER");
                    //toDo restart handshake timeout ????
                    return STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.RAT_PROVER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_PROVER failed");
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Send IDSC_CLOSE");
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_PROVER failed", IdscpClose.CloseCause.RAT_PROVER_FAILED));
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(InternalControlMessage.RAT_PROVER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_PROVER");
                    sendFromFSM(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    stopRatProverDriver();
                    this.handshakeTimer.cancelTimeout();
                    LOG.debug("Switch to state STATE_CLOSED");
                    notifyHandshakeCompleteLock();
                    return STATE_CLOSED;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(IdscpMessage.IDSCPDAT_FIELD_NUMBER, new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Verify received DAT");
                    //check if Dat is available and verify dat
                    byte[] dat = event.getIdscpMessage().getIdscpDat().getToken().toByteArray();
                    int datValidityPeriod;
                    if (0 > (datValidityPeriod = this.dapsDriver.verifyToken(dat))){
                        stopRatProverDriver();
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No valid DAT", IdscpClose.CloseCause.NO_VALID_DAT));
                        LOG.debug("Switch to state STATE_CLOSED");
                        notifyHandshakeCompleteLock();
                        return STATE_CLOSED;
                    }
                    this.remoteDat = dat;
                    LOG.debug("Remote DAT is valid. Set dat timeout");
                    datTimer.resetTimeout(datValidityPeriod);
                    //start RAT Verifier
                    restartRatVerifierDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT");
                    return STATE_WAIT_FOR_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER");
                    ratProverDriver.delegate(event.getIdscpMessage());
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Restart RAT_PROVER");
                    restartRatProverDriver();
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        ));

        STATE_WAIT_FOR_DAT_AND_RAT.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_DAT_AND_RAT");
                    return STATE_WAIT_FOR_DAT_AND_RAT;
                }
        );

        /*---------------------------------------------------
         * STATE_ESTABLISHED - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {timeouts.cancel(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: stop ---> {timeouts.cancel(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: re_rat ---> {send IDSCP_RE_RAT, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * //FIXME onICM: send_data ---> {send IDS_DATA} ---> STATE_ESTABLISHED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onMessage: IDSCP_DATA ---> {delegate to connection} ---> STATE_ESTABLISHED
         * onMessage: IDSCP_RERAT ---> {start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_CLOSE ---> {timeouts.cancel()} ---> STATE_CLOSED
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_ESTABLISHED
         * --------------------------------------------------- */
        STATE_ESTABLISHED.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("Error occurred, send IDSCP_CLOSE and close idscp connection");
                    this.datTimer.cancelTimeout();
                    this.ratTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Error occurred", IdscpClose.CloseCause.ERROR));
                    LOG.debug("Switch to state STATE_CLOSED");
                    return STATE_CLOSED;
                }
        ));

        STATE_ESTABLISHED.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_CLOSE");
                    this.datTimer.cancelTimeout();
                    this.ratTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close", IdscpClose.CloseCause.USER_SHUTDOWN));
                    LOG.debug("Switch to state STATE_CLOSED");
                    return STATE_CLOSED;
                }
        ));

        STATE_ESTABLISHED.addTransition(InternalControlMessage.REPEAT_RAT.getValue(), new Transition(
                event -> {
                    LOG.debug("Request RAT repeat. Send IDSCP_RERAT, start RAT_VERIFIER");
                    this.ratTimer.cancelTimeout();
                    sendFromFSM(IdscpMessageFactory.getIdscpReRatMessage(""));
                    restartRatVerifierDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_VERIFIER");
                    return STATE_WAIT_FOR_RAT_VERIFIER;
                }
        ));

        STATE_ESTABLISHED.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    this.ratTimer.cancelTimeout();
                    LOG.debug("Remote DAT expired. Send IDSCP_DAT_EXPIRED");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage());

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_DAT_AND_RAT_CLIENT");
                    return STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER;
                }
        ));

        STATE_ESTABLISHED.addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RERAT. Start RAT_PROVER");
                    restartRatProverDriver();
                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_ESTABLISHED.addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("DAT expired. Send new DAT and repeat RAT");
                    sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(this.dapsDriver.getToken()));
                    restartRatProverDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    LOG.debug("Switch to state STATE_WAIT_FOR_RAT_PROVER");
                    return STATE_WAIT_FOR_RAT_PROVER;
                }
        ));

        STATE_ESTABLISHED.addTransition(IdscpMessage.IDSCPDATA_FIELD_NUMBER, new Transition(
                event -> {
                    try {
                        this.listenerLatch.await();
                        this.listener.onMessage(event.getIdscpMessage());
                        LOG.debug("Idscp data was passed to connection listener");
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    LOG.debug("Stay in state STATE_ESTABLISHED");
                    return STATE_ESTABLISHED;
                }
        ));

        STATE_ESTABLISHED.addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Receive IDSCP_CLOSED");
                    datTimer.cancelTimeout();
                    ratTimer.cancelTimeout();
                    LOG.debug("Switch to STATE_CLOSED");
                    return STATE_CLOSED;
                }
        ));

        STATE_ESTABLISHED.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_ESTABLISHED");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
        IdscpMessage message;
        try {
            message = IdscpMessage.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}", data);
            return;
        }

        Event event = new Event(message);
        //must wait when fsm is in state STATE_CLOSED --> wait() will be notified when fsm is leaving STATE_CLOSED
        synchronized (fsmIsBusy){
            while (currentState == STATE_CLOSED){
                try {
                    fsmIsBusy.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            currentState = currentState.feedEvent(event);
        }
    }

    @Override
    public void onControlMessage(InternalControlMessage controlMessage) {
        //create Internal Control Message Event and pass it to current state and update new state
        Event e = new Event(controlMessage);
        synchronized (fsmIsBusy){
            currentState = currentState.feedEvent(e);
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

        synchronized (fsmIsBusy) {
            if (Long.toString(Thread.currentThread().getId()).equals(currentRatProverId)) {
                currentState = currentState.feedEvent(e);
            }
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

        synchronized (fsmIsBusy) {
            if (Long.toString(Thread.currentThread().getId()).equals(currentRatVerifierId)) {
                currentState = currentState.feedEvent(e);
            }
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

    private void sendFromFSM(IdscpMessage msg){
        //send messages from fsm
        secureChannel.send(msg.toByteArray());
    }

    public void send(IdscpMessage msg){
        //send messages from user only when idscp connection is established
        synchronized(fsmIsBusy){
            if(isConnected()){
                secureChannel.send(msg.toByteArray());
            } else {
                LOG.error("Cannot send IDSCP_DATA because protocol is not established");
            }
        }
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

    //calculate Prover mechanism (strongest remote expected), returns null if no match was found
    private String getRatProverMechanism(String[] localSupportedProver, Object[] remoteExpectedVerifier){
        //toDo implement logic
        return "TPM_2";
    }

    //calculate Verifier mechanism (strongest local expected), returns null if no match was found
    private String getRatVerifierMechanism(String[] localExpectedVerifier, Object[] remoteSupportedProver){
        //toDo implement logic
        return "TPM_2";
    }

    private void restartRatVerifierDriver(){
        //assume verifier mechanism is set
        stopRatVerifierDriver();
        ratVerifierDriver = RatVerifierDriverRegistry.startRatVerifierDriver(verifierMechanism, this);
        if (ratVerifierDriver == null){
            LOG.error("Cannot create instance of RAT_VERIFIER_DRIVER");
            currentRatVerifierId = "";
            onControlMessage(InternalControlMessage.ERROR); //toDo geht das?
        } else {
            currentRatVerifierId = Long.toString(ratVerifierDriver.getId());
        }
    }

    private void stopRatVerifierDriver(){
        if (ratVerifierDriver != null && ratVerifierDriver.isAlive()){
            ratVerifierDriver.interrupt();
            ratVerifierDriver.terminate();
        }
    }

    private void restartRatProverDriver(){
        //assume prover mechanism is set
        stopRatProverDriver();
        ratProverDriver = RatProverDriverRegistry.startRatProverDriver(proverMechanism, this);
        if (ratProverDriver == null){
            LOG.error("Cannot create instance of RAT_PROVER_DRIVER");
            currentRatProverId = "";
            onControlMessage(InternalControlMessage.ERROR); //toDo geht das?
        } else {
            currentRatProverId = Long.toString(ratProverDriver.getId());
        }
    }

    private void stopRatProverDriver(){
        if (ratProverDriver != null && ratProverDriver.isAlive()){
            ratProverDriver.interrupt();
            ratProverDriver.terminate();
        }
    }
}
