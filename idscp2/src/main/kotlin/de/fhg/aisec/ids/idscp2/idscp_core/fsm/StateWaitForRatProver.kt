package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

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
                            ackTimer: StaticTimer) : State() {
    override fun runEntryCode(fsm: FSM) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_WAIT_FOR_RAT_PROVER")
        }
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
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Send IDSC_CLOSE")
            }
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close", CloseCause.USER_SHUTDOWN))
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.warn("An internal control error occurred")
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.SEND_DATA.value, Transition {
            FSM.FsmResult(FSM.FsmResultCode.NOT_CONNECTED, this)
        })

        addTransition(InternalControlMessage.TIMEOUT.value, Transition {
            LOG.warn("Handshake timeout occurred. Send IDSCP_CLOSE")
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "Handshake timeout",
                    CloseCause.TIMEOUT
                )
            )
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("DAT timeout occurred. Send IDSCP_DAT_EXPIRED")
            }
            ratTimer.cancelTimeout()
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                LOG.warn("Cannot send DatExpired message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (LOG.isTraceEnabled) {
                LOG.trace("Start Handshake Timer")
            }
            handshakeTimer.resetTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT))
        })

        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received RAT_PROVER OK")
            }
            proverHandshakeTimer.cancelTimeout()
            if (fsm.ackFlag) {
                ackTimer.start()
                return@Transition FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_ACK))
            } else {
                return@Transition FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_ESTABLISHED))
            }
        })

        addTransition(InternalControlMessage.RAT_PROVER_FAILED.value, Transition {
            LOG.warn("RAT_PROVER failed. Send IDSCP_CLOSE")
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "RAT_PROVER failed",
                    CloseCause.RAT_PROVER_FAILED
                )
            )
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.RAT_PROVER_MSG.value, Transition { event: Event ->
            if (LOG.isTraceEnabled) {
                LOG.trace("Send IDSCP_RAT_PROVER")
            }
            if (!fsm.sendFromFSM(event.idscpMessage)) {
                LOG.warn("Cannot send rat prover message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Request RAT repeat. Send IDSCP_RE_RAT, start RAT_VERIFIER")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpReRatMessage(""))) {
                LOG.warn("Cannot send ReRat message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            ratTimer.cancelTimeout()
            if (!fsm.restartRatVerifierDriver()) {
                LOG.warn("Cannot run Rat verifier, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT))
        })

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_CLOSE")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(fsm.getDynamicAttributeToken))) {
                LOG.warn("Cannot send DAT message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, Transition { event: Event ->
            if (LOG.isTraceEnabled) {
                LOG.trace("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER")
            }

            if (!event.idscpMessage.hasIdscpRatVerifier()) {
                // this should never happen
                LOG.warn("IDSCP_RAT_VERIFIER Message not available")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            fsm.ratProverDriver?.let {
                // Run in async fire-and-forget coroutine to avoid cycles caused by protocol misuse
                GlobalScope.launch {
                    it.delegate(event.idscpMessage.idscpRatVerifier.data.toByteArray())
                }
            } ?: run {
                LOG.warn("RatProverDriver not available")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_RE_RAT. Restart RAT_PROVER")
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition {
            fsm.recvAck(it.idscpMessage.idscpAck)
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        setNoTransitionHandler {
            if (LOG.isTraceEnabled) {
                LOG.trace("No transition available for given event $it")
                LOG.trace("Stay in state STATE_WAIT_FOR_RAT_PROVER")
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }
    }
}