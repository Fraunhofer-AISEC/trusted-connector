package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateWaitForDatAndRatVerifier extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateWaitForDatAndRatVerifier.class);

    private final Timer handshakeTimer;

    public StateWaitForDatAndRatVerifier(FSM fsm,
                                         Timer handshakeTimer,
                                         Timer datTimer,
                                         DapsDriver dapsDriver){

        this.handshakeTimer = handshakeTimer;
        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: error ---> {} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_CLOSE ---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT(success) --> {verify DAT, set det_timeout, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFEIER
         * onMessage: IDSCP_DAT(failed) --> {verify DAT, send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RE_RAT ---> {start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
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
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("Handshake timeout",
                            IDSCPv2.IdscpClose.CloseCause.TIMEOUT));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_CLOSE");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDAT_FIELD_NUMBER, new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();
                    LOG.debug("Verify received DAT");
                    //check if Dat is available and verify dat
                    byte[] dat = event.getIdscpMessage().getIdscpDat().getToken().toByteArray();
                    int datValidityPeriod;
                    if (0 > (datValidityPeriod = dapsDriver.verifyToken(dat))){
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("No valid DAT", IDSCPv2.IdscpClose.CloseCause.NO_VALID_DAT));
                        return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                    }
                    LOG.debug("Remote DAT is valid. Set dat timeout");
                    datTimer.resetTimeout(datValidityPeriod);
                    //start RAT Verifier
                    fsm.restartRatVerifierDriver();

                    LOG.debug("Set handshake timeout");
                    handshakeTimer.resetTimeout(5);

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT_VERIFIER);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(dapsDriver.getToken()));
                    fsm.restartRatProverDriver();
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER");
                    fsm.restartRatProverDriver();
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT);
                }
        ));

        this.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    return this;
                }
        );
    }

    @Override
    void runEntryCode(FSM fsm){
        LOG.debug("Switched to state STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER");
        LOG.debug("Set handshake timeout");
        handshakeTimer.resetTimeout(5);
    }
}
