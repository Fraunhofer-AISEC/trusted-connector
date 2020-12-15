package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * The Wait_For_Rat State of the FSM of the IDSCP2 protocol.
 * Waits for the RatProver and RatVerifier Result to decide whether the connection will be
 * established
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForRat(fsm: FSM,
                      handshakeTimer: StaticTimer,
                      verifierHandshakeTimer: StaticTimer,
                      proverHandshakeTimer: StaticTimer,
                      ratTimer: StaticTimer) : State() {
    override fun runEntryCode(fsm: FSM) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_WAIT_FOR_RAT")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForRat::class.java)
    }

    init {


        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {ratP.stop(), ratV.stop(), timeouts.stop()} ---> IDSCP_CLOSED
         * onICM: stop ---> {ratP.stop(), ratV.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> IDSCP_CLOSED
         * onICM: rat_prover_ok ---> {} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onICM: rat_verifier_ok ---> {set rat timeout} ---> STATE_WAIT_FOR_RAT_PROVER
         * onICM: rat_prover_failed ---> {ratV.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_verifier_failed ---> {ratP.stop(), timeouts.stop(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onICM: rat_verifier_msg ---> {send IDSCP_RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onICM: dat_timeout ---> {send DAT_EXPIRED, ratV.cancel()} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onICM: handshake_timeout ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_ACK ---> {cancel Ack flag} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_RAT_PROVER ---> {delegate to RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_DAT_EXPIRED ---> {send DAT, ratP.restart()} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {ratP.stop(), ratV.stop(), timeouts.stop()} ---> IDSCP_CLOSED
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.warn("An internal control error occurred")
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Send IDSCP_CLOSE")
            }
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "User close",
                    CloseCause.USER_SHUTDOWN
                )
            )
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.SEND_DATA.value, Transition {
            FSM.FsmResult(FSM.FsmResultCode.NOT_CONNECTED, this)
        })

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            // nothing to do, currently attestating
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received RAT_PROVER OK")
            }
            proverHandshakeTimer.cancelTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER))
        })

        addTransition(InternalControlMessage.RAT_VERIFIER_OK.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received RAT_VERIFIER OK")
            }
            verifierHandshakeTimer.cancelTimeout()
            ratTimer.resetTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER))
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

        addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.value, Transition {
            LOG.warn("RAT_VERIFIER failed. Send IDSCP_CLOSE")
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "RAT_VERIFIER failed",
                    CloseCause.RAT_VERIFIER_FAILED
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

        addTransition(InternalControlMessage.RAT_VERIFIER_MSG.value, Transition { event: Event ->
            if (LOG.isTraceEnabled) {
                LOG.trace("Send IDSCP_RAT_VERIFIER")
            }
            if (!fsm.sendFromFSM(event.idscpMessage)) {
                LOG.warn("Cannot send rat verifier message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("DAT timeout, send IDSCP_DAT_EXPIRED and cancel RAT_VERIFIER")
            }
            fsm.stopRatVerifierDriver()
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

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition {
            fsm.recvAck(it.idscpMessage.idscpAck)
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, Transition { event: Event ->
            if (LOG.isTraceEnabled) {
                LOG.trace("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER")
            }

            if (!event.idscpMessage.hasIdscpRatVerifier()) {
                // this should never happen
                LOG.warn("IDSCP_RAT_Verifier message not available")
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

        addTransition(IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, Transition { event: Event ->
            if (LOG.isTraceEnabled) {
                LOG.trace("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER")
            }

            if (!event.idscpMessage.hasIdscpRatProver()) {
                // this should never happen
                LOG.warn("IDSCP_RAT_PROVER message not available")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            fsm.ratVerifierDriver?.let {
                // Run in async fire-and-forget coroutine to avoid cycles caused by protocol misuse
                GlobalScope.launch {
                    it.delegate(event.idscpMessage.idscpRatProver.data.toByteArray())
                }
            } ?: run {
                LOG.warn("RatVerifierDriver not available")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            FSM.FsmResult(FSM.FsmResultCode.OK, this)
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

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_CLOSE")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        setNoTransitionHandler { event: Event? ->
            if (LOG.isTraceEnabled) {
                LOG.trace("No transition available for given event " + event.toString())
                LOG.trace("Stay in state STATE_WAIT_FOR_RAT")
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }
    }
}