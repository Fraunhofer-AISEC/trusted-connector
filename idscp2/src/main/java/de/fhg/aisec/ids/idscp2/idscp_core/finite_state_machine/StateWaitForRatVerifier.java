package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateWaitForRatVerifier extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateWaitForRatVerifier.class);

    public StateWaitForRatVerifier(FSM fsm,
                                   DapsDriver dapsDriver,
                                   Timer ratTimer,
                                   Timer handshakeTimer,
                                   int ratTimeoutDelay) {

        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {stop RAT_VERIFIER} ---> STATE_CLOSED
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
        this.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSC_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close",
                            IDSCPv2.IdscpClose.CloseCause.USER_SHUTDOWN));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout", IDSCPv2.IdscpClose.CloseCause.TIMEOUT));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    LOG.debug("DAT timeout occurred. Send IDSCP_DAT_EXPIRED and stop RAT_VERIFIER");
                    fsm.stopRatVerifierDriver();
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage());
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER);
                }
        ));

        this.addTransition(InternalControlMessage.RAT_VERIFIER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_VERIFIER OK");
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Start RAT Timer");
                    ratTimer.resetTimeout(ratTimeoutDelay);
                    return fsm.getState(FSM.FSM_STATE.STATE_ESTABLISHED);
                }
        ));

        this.addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_VERIFIER failed");
                    LOG.debug("Send IDSC_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_VERIFIER failed",
                            IDSCPv2.IdscpClose.CloseCause.RAT_VERIFIER_FAILED));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.RAT_VERIFIER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_VERIFIER");
                    fsm.sendFromFSM(event.getIdscpMessage());
                    return this;
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(dapsDriver.getToken()));
                    fsm.restartRatProverDriver();
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER");
                    fsm.getRatVerifierDriver().delegate(event.getIdscpMessage());
                    return this;
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER");
                    fsm.restartRatProverDriver();
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT);
                }
        ));

        this.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_VERIFIER");
                    return this;
                }
        );
    }

    @Override
    void runEntryCode(FSM fsm){
        LOG.debug("Switched to state STATE_WAIT_FOR_RAT_VERIFIER");
    }
}
