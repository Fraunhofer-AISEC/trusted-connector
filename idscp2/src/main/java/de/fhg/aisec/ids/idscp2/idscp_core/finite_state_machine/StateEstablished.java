package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM.FSM_STATE;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Established State of the FSM of the IDSCPv2 protocol.
 * Allows message exchange over the IDSCPv2 protocol between two connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class StateEstablished extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateEstablished.class);

    public StateEstablished(FSM fsm,
                            DapsDriver dapsDriver,
                            Timer ratTimer,
                            Timer handshakeTimer) {


        /*---------------------------------------------------
         * STATE_ESTABLISHED - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {timeouts.cancel(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: stop ---> {timeouts.cancel()} ---> STATE_CLOSED
         * onICM: re_rat ---> {send IDSCP_RE_RAT, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * //onICM: send_data ---> {send IDS_DATA} ---> STATE_ESTABLISHED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onMessage: IDSCP_DATA ---> {delegate to connection} ---> STATE_ESTABLISHED
         * onMessage: IDSCP_RERAT ---> {start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_CLOSE ---> {timeouts.cancel()} ---> STATE_CLOSED
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_ESTABLISHED
         * --------------------------------------------------- */
        this.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("Error occurred, close idscp connection");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    LOG.debug("Send IDSCP_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.getIdscpCloseMessage("User close",
                            IDSCPv2.IdscpClose.CloseCause.USER_SHUTDOWN));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.REPEAT_RAT.getValue(), new Transition(
                event -> {
                    LOG.debug("Request RAT repeat. Send IDSCP_RERAT, start RAT_VERIFIER");
                    ratTimer.cancelTimeout();

                    if (!fsm.sendFromFSM(IdscpMessageFactory.getIdscpReRatMessage(""))) {
                      LOG.error("Cannot send ReRat message");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    if (!fsm.restartRatVerifierDriver()) {
                      LOG.error("Cannot run Rat verifier, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT_VERIFIER);
                }
        ));

        this.addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.getValue(), new Transition(
                event -> {
                    ratTimer.cancelTimeout();
                    LOG.debug("Remote DAT expired. Send IDSCP_DAT_EXPIRED");
                    if (!fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatExpiredMessage())) {
                      LOG.error("Cannot send DatExpired message");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                  LOG.debug("Set handshake timeout");
                  handshakeTimer.resetTimeout(5);

                  return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPRERAT_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Received IDSCP_RERAT. Start RAT_PROVER");

                    if (!fsm.restartRatProverDriver()) {
                      LOG.error("Cannot run Rat prover, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT_PROVER);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("DAT expired. Send new DAT and repeat RAT");

                    if (!fsm.sendFromFSM(IdscpMessageFactory.getIdscpDatMessage(dapsDriver.getToken()))) {
                      LOG.error("Cannot send Dat message");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    if (!fsm.restartRatProverDriver()) {
                      LOG.error("Cannot run Rat prover, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT_PROVER);
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPDATA_FIELD_NUMBER, new Transition(
                event -> {
                    fsm.notifyIdscpMsgListener(event.getIdscpMessage().getIdscpData().toByteArray());
                    return this;
                }
        ));

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, new Transition(
                event -> {
                    LOG.debug("Receive IDSCP_CLOSED");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_ESTABLISHED");
                    return this;
                }
        );
    }

    @Override
    void runEntryCode(FSM fsm){
        LOG.debug("Switched to state STATE_ESTABLISHED");
        fsm.notifyHandshakeCompleteLock();
    }
}
