package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.idscp_core.IdscpMessageFactory;
import de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine.FSM.FSM_STATE;
import de.fhg.aisec.ids.messages.IDSCPv2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Wait_For_Hello State of the FSM of the IDSCPv2 protocol.
 * Waits for the Idscpv2 Hellp Message that contains the protocol version, the supported and
 * expected remote attestation cipher suites and the dynamic attribute token (DAT) of the peer.
 *
 * Goes into the WAIT_FOR_RAT State when valid Rat mechanisms were found and the DAT is valid
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class StateWaitForHello extends State {
    private static final Logger LOG = LoggerFactory.getLogger(StateWaitForHello.class);

    private final Timer handshakeTimer;

    public StateWaitForHello(FSM fsm,
                             Timer handshakeTimer,
                             Timer datTimer,
                             DapsDriver dapsDriver,
                             String[] localSupportedRatSuite,
                             String[] localExpectedRatSuite) {

        this.handshakeTimer = handshakeTimer;

        /*---------------------------------------------------
         * STATE_WAIT_FOR_HELLO - Transition Description
         * ---------------------------------------------------
         * onICM: error --> {} ---> STATE_CLOSED
         * onICM: stop --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: timeout --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_CLOSE---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (no rat match) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (invalid DAT) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (SUCCESS) ---> {verify DAT, match RAT, set DAT Timeout, start RAT P&V,
         *                                        set handshake_timeout} ---> STATE_WAIT_FOR_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_HELLO
         * --------------------------------------------------- */
        this.addTransition(InternalControlMessage.ERROR.getValue(), new Transition(
                event -> {
                    LOG.debug("An internal control error occurred");
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.IDSCP_STOP.getValue(), new Transition(
                event -> {
                    LOG.debug("Received stop signal from user. Send IDSC_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.createIdscpCloseMessage("User close",
                            IDSCPv2.IdscpClose.CloseCause.USER_SHUTDOWN));
                    return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                }
        ));

        this.addTransition(InternalControlMessage.TIMEOUT.getValue(), new Transition(
                event -> {
                    LOG.debug("STATE_WAIT_FOR_HELLO timeout. Send IDSCP_CLOSE");
                    fsm.sendFromFSM(IdscpMessageFactory.createIdscpCloseMessage("Handshake Timeout",
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

        this.addTransition(IDSCPv2.IdscpMessage.IDSCPHELLO_FIELD_NUMBER, new Transition(
                event -> {
                    handshakeTimer.cancelTimeout();

                    LOG.debug("Received IDSCP_HELLO");
                    IDSCPv2.IdscpHello idscpHello = event.getIdscpMessage().getIdscpHello();

                    LOG.debug("Calculate Rat mechanisms");
                    String proverMechanism = fsm.getRatProverMechanism(localSupportedRatSuite,
                            idscpHello.getExpectedRatSuiteList().toArray());
                    if (proverMechanism == null){
                        LOG.debug("Cannot find a match for RAT proverr. Send IDSCP_CLOSE");
                        fsm.sendFromFSM(IdscpMessageFactory.createIdscpCloseMessage("No match for RAT Prover mechanism",
                                IDSCPv2.IdscpClose.CloseCause.NO_RAT_MECHANISM_MATCH_PROVER));
                        return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                    }

                    String verifierMechanism = fsm.getRatVerifierMechanism(localExpectedRatSuite,
                            idscpHello.getSupportedRatSuiteList().toArray());
                    if (verifierMechanism == null){
                        LOG.debug("Cannot find a match for RAT verifier. Send IDSCP_CLOSE");
                        fsm.sendFromFSM(IdscpMessageFactory.createIdscpCloseMessage("No match for RAT Verifier mechanism",
                                IDSCPv2.IdscpClose.CloseCause.NO_RAT_MECHANISM_MATCH_VERIFIER));
                        return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                    }

                    LOG.debug("Verify received DAT");
                    //check if Dat is available and verify dat
                    byte[] dat;
                    long datValidityPeriod;
                    if (!idscpHello.hasDynamicAttributeToken() || 0 > (datValidityPeriod = dapsDriver
                            .verifyToken(dat = idscpHello.getDynamicAttributeToken().getToken().toByteArray(), null))){
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE");
                        fsm.sendFromFSM(IdscpMessageFactory.createIdscpCloseMessage("No valid DAT",
                                IDSCPv2.IdscpClose.CloseCause.NO_VALID_DAT));
                        return fsm.getState(FSM.FSM_STATE.STATE_CLOSED);
                    }

                    LOG.debug("Remote DAT is valid. Set dat timeout to its validity period");
                    datTimer.resetTimeout(datValidityPeriod);

                    fsm.setRatMechanisms(proverMechanism, verifierMechanism);

                    LOG.debug("Start RAT Prover and Verifier");

                    if (!fsm.restartRatVerifierDriver()) {
                      LOG.error("Cannot run Rat verifier, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    if (!fsm.restartRatProverDriver()) {
                      LOG.error("Cannot run Rat prover, close idscp connection");
                      return fsm.getState(FSM_STATE.STATE_CLOSED);
                    }

                    return fsm.getState(FSM.FSM_STATE.STATE_WAIT_FOR_RAT);
                }
        ));

        this.setNoTransitionHandler(
                event -> {
                    LOG.debug("No transition available for given event " + event.toString());
                    LOG.debug("Stay in state STATE_WAIT_FOR_HELLO");
                    return this;
                }
        );
    }


    @Override
    void runEntryCode(FSM fsm) {
        LOG.debug("Switched to state STATE_WAIT_FOR_HELLO");
        LOG.debug("Set handshake timeout to 5 seconds");
        handshakeTimer.resetTimeout(5);
    }


}
