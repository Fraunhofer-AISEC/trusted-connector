package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM.FSM_STATE;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Wait_For_Dat_And_Rat State of the FSM of the IDSCPv2 protocol.
 * Waits for a new valid dynamic attribute token from the peer as well as for the RatProver and
 * RatVerifier to decide whether the connection will be established or not
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class StateWaitForDatAndRat extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateWaitForDatAndRat.class);

    public StateWaitForDatAndRat(FSM fsm,
                                 Timer handshakeTimer,
                                 Timer proverHandshakeTimer,
                                 Timer datTimer,
                                 DapsDriver dapsDriver
                                 ){

        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: error ---> {stop RAT_PROVER} ---> STATE_CLOSED
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

        this.addTransition(InternalControlMessage.RAT_PROVER_OK.getValue(), new Transition(
                event -> {
                    LOG.debug("Received RAT_PROVER OK");
                    proverHandshakeTimer.cancelTimeout();
                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER);
                }
        ));

        this.addTransition(InternalControlMessage.RAT_PROVER_FAILED.getValue(), new Transition(
                event -> {
                    LOG.error("RAT_PROVER failed");
                    LOG.debug("Send IDSC_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("RAT_PROVER failed",
                            IDSCPv2.IdscpClose.CloseCause.RAT_PROVER_FAILED));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.RAT_PROVER_MSG.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_RAT_PROVER");
                    if (!fsm.sendFromFSM(event.getIdscpMessage())) {
                      LOG.error("Cannot send rat prover message");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }
                    return this;
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
                    long datValidityPeriod;
                    if (0 > (datValidityPeriod = dapsDriver.verifyToken(dat, null))){
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage(
                            "No valid DAT", IDSCPv2.IdscpClose.CloseCause.NO_VALID_DAT));
                        return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                    }

                    LOG.debug("Remote DAT is valid. Set dat timeout");
                    datTimer.resetTimeout(datValidityPeriod);
                    //start RAT Verifier
                    if (!fsm.restartRatVerifierDriver()) {
                      LOG.error("Cannot run Rat verifier, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER");

                    if (!fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(dapsDriver.getToken()))) {
                      LOG.error("Cannot send Dat message");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    if (!fsm.restartRatProverDriver()) {
                      LOG.error("Cannot run Rat prover, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return this;
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER");
                    fsm.getRatProverDriver().delegate(event.getIdscpMessage());
                    return this;
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RE_RAT. Restart RAT_PROVER");
                    if (!fsm.restartRatProverDriver()) {
                      LOG.error("Cannot run Rat prover, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }
                    return this;
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
        LOG.debug("Switched to state STATE_WAIT_FOR_DAT_AND_RAT");
    }
}
