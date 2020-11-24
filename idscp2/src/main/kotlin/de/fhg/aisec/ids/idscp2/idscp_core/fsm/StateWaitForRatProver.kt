package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * The Wait_For_Rat_Prover State of the FSM of the IDSCP2 protocol.
 * Waits only for the RatProver Result to decide whether the connection will be established
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForRatProver(fsm: FSM,
                            ratTimer: StaticTimer,
                            handshakeTimer: StaticTimer,
                            proverHandshakeTimer: StaticTimer,
                            dapsDriver: DapsDriver,
                            ackTimer: StaticTimer) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_RAT_PROVER")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForRatProver::class.java)
    }

    init {


        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT_PROVER - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE, stop RAT_PROVER, timeouts.terminate()} ---> STATE_CLOSED
         * onICM: error ---> {stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onICM: rat_prover_ok ---> {} ---> STATE_ESTABLISHED / STATE_WAIT_FOR_ACK
         * onICM: rat_prover_failed ---> {send IDSCP_CLOSE, terminate ratP, cancel timeouts} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onICM: repeat_rat ---> {send IDSCP_RE_RAT, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_ACK ---> {cancel Ack flag} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, restart RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_RE_RAT ---> {restart RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT_PROVER
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition (
                Function {
                    LOG.debug("Send IDSC_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close", CloseCause.USER_SHUTDOWN))
                    fsm.getState(FsmState.STATE_CLOSED)
                }
        ))

        addTransition(InternalControlMessage.ERROR.value, Transition (
                Function {
                    LOG.debug("An internal control error occurred")
                    fsm.getState(FsmState.STATE_CLOSED)
                }
        ))

        addTransition(InternalControlMessage.TIMEOUT.value, Transition (
                Function {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("Handshake timeout",
                            CloseCause.TIMEOUT))
                    fsm.getState(FsmState.STATE_CLOSED)
                }
        ))

        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition(
                Function {
                    LOG.debug("DAT timeout occurred. Send IDSCP_DAT_EXPIRED")
                    ratTimer.cancelTimeout()
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                        LOG.error("Cannot send DatExpired message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    LOG.debug("Start Handshake Timer")
                    handshakeTimer.resetTimeout()
                    fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT)
                }
        ))

        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition (
                Function {
                    LOG.debug("Received RAT_PROVER OK")
                    proverHandshakeTimer.cancelTimeout()
                    if (fsm.getAckFlag) {
                        ackTimer.start()
                        return@Function fsm.getState(FsmState.STATE_WAIT_FOR_ACK)
                    } else {
                        return@Function fsm.getState(FsmState.STATE_ESTABLISHED)
                    }
                }
        ))

        addTransition(InternalControlMessage.RAT_PROVER_FAILED.value, Transition (
                Function {
                    LOG.error("RAT_PROVER failed")
                    LOG.debug("Send IDSC_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("RAT_PROVER failed",
                            CloseCause.RAT_PROVER_FAILED))
                    fsm.getState(FsmState.STATE_CLOSED)
                }
        ))

        addTransition(InternalControlMessage.RAT_PROVER_MSG.value, Transition(
                Function { event: Event ->
                    LOG.debug("Send IDSCP_RAT_PROVER")
                    if (!fsm.sendFromFSM(event.idscpMessage)) {
                        LOG.error("Cannot send rat prover message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    this
                }
        ))

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition(
                Function {
                    LOG.debug("Request RAT repeat. Send IDSCP_RE_RAT, start RAT_VERIFIER")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpReRatMessage(""))) {
                        LOG.error("Cannot send ReRat message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    ratTimer.cancelTimeout()
                    if (!fsm.restartRatVerifierDriver()) {
                        LOG.error("Cannot run Rat verifier, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT)
                }
        ))

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition (
                Function {
                    LOG.debug("Received IDSCP_CLOSE")
                    fsm.getState(FsmState.STATE_CLOSED)
                }
        ))

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(dapsDriver.token))) {
                        LOG.error("Cannot send DAT message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    this
                }
        ))

        addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, Transition (
                Function { event: Event ->
                    LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER")
                    assert(event.idscpMessage.hasIdscpRatVerifier())
                    fsm.ratProverDriver?.delegate(event.idscpMessage.idscpRatVerifier.data.toByteArray())
                            ?: throw Idscp2Exception("RAT prover driver not available")
                    this
                }
        ))

        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_RE_RAT. Restart RAT_PROVER")
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    this
                }
        ))

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition (
                Function {
                    fsm.recvAck(it.idscpMessage.idscpAck)
                    this
                }
        ))

        setNoTransitionHandler (
                Function {
                    LOG.debug("No transition available for given event $it")
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_PROVER")
                    this
                }
        )
    }
}