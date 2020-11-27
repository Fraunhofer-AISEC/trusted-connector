package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * The Wait_For_Rat_Verifier State of the FSM of the IDSCP2 protocol.
 * Waits only for the RatVerifier Result to decide whether the connection will be established
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForRatVerifier(fsm: FSM,
                              ratTimer: StaticTimer,
                              handshakeTimer: StaticTimer,
                              verifierHandshakeTimer: StaticTimer,
                              ackTimer: StaticTimer) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_RAT_VERIFIER")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForRatVerifier::class.java)
    }

    init {

        /*---------------------------------------------------
         * STATE_WAIT_FOR_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: close ---> {send IDSCP_CLOSE, stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED, cancel ratV} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_VERIFIER} ---> STATE_CLOSED
         * onICM: rat_verifier_ok ---> {set rat timeout} ---> STATE_ESTABLISHED / STATE_WAIT_FOR_ACK
         * onICM: rat_verifier_failed ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_verifier_msg ---> {send IDSCP_RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onMessage: IDSCP_ACK ---> {cancel Ack flag} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {stop RAT_VERIFIER} ---> STATE_CLOSED
         * onMessage: IDSCP_RAT_PROVER ---> {delegat to RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onMessage: IDSCP_RE_RAT ---> {start RAT_PROVER} ---> STATE_WAIT_FOR_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition (
                Function {
                    LOG.debug("Send IDSC_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close",
                            CloseCause.USER_SHUTDOWN))
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED)!!)
                }
        ))

        addTransition(InternalControlMessage.ERROR.value, Transition (
                Function {
                    LOG.debug("An internal control error occurred")
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED)!!)
                }
        ))

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition (
                Function {
                    // already re-attestating
                    FSM.FsmResult(FSM.FsmResultCode.OK, this)
                }
        ))

        addTransition(InternalControlMessage.SEND_DATA.value, Transition (
                Function {
                    FSM.FsmResult(FSM.FsmResultCode.NOT_CONNECTED, this)
                }
        ))

        addTransition(InternalControlMessage.TIMEOUT.value, Transition (
                Function {
                    LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("Handshake timeout", CloseCause.TIMEOUT))
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED)!!)
                }
        ))

        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition(
                Function {
                    LOG.debug("DAT timeout occurred. Send IDSCP_DAT_EXPIRED and stop RAT_VERIFIER")
                    fsm.stopRatVerifierDriver()
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                        LOG.error("Cannot send DatExpired message")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }
                    LOG.debug("Start Handshake Timer")
                    handshakeTimer.resetTimeout()
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER)!!)
                }
        ))

        addTransition(InternalControlMessage.RAT_VERIFIER_OK.value, Transition (
                Function {
                    LOG.debug("Received RAT_VERIFIER OK")
                    verifierHandshakeTimer.cancelTimeout()
                    LOG.debug("Start RAT Timer")
                    ratTimer.resetTimeout()
                    if (fsm.getAckFlag) {
                        ackTimer.start()
                        return@Function FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_ACK)!!)
                    } else {
                        return@Function FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_ESTABLISHED)!!)
                    }
                }
        ))

        addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.value, Transition (
                Function {
                    LOG.error("RAT_VERIFIER failed")
                    LOG.debug("Send IDSC_CLOSE")
                    fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("RAT_VERIFIER failed",
                            CloseCause.RAT_VERIFIER_FAILED))
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED)!!)
                }
        ))

        addTransition(InternalControlMessage.RAT_VERIFIER_MSG.value, Transition(
                Function { event: Event ->
                    LOG.debug("Send IDSCP_RAT_VERIFIER")
                    if (!fsm.sendFromFSM(event.idscpMessage)) {
                        LOG.error("Cannot send rat verifier message")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }
                    FSM.FsmResult(FSM.FsmResultCode.OK, this)
                }
        ))

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition (
                Function {
                    LOG.debug("Received IDSCP_CLOSE")
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED)!!)
                }
        ))

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(fsm.getDynamicAttributeToken))) {
                        LOG.error("Cannot send DAT message")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT)!!)
                }
        ))

        addTransition(IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, Transition (
                Function { event: Event ->
                    LOG.debug("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER")

                    if(!event.idscpMessage.hasIdscpRatProver()) {
                        // this should never happen
                        LOG.error("IDSCP_RAT_PROVER Message not available")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }

                    if (fsm.ratVerifierDriver == null) {
                        LOG.error("RatVerifierDriver not available")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }

                    // run in async fire-and-forget coroutine to avoid cycles caused by protocol misuse
                    GlobalScope.launch {
                        fsm.ratVerifierDriver!!.delegate(event.idscpMessage.idscpRatProver.data.toByteArray())
                    }

                    FSM.FsmResult(FSM.FsmResultCode.OK,this)
                }
        ))

        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER")
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED)!!)
                    }
                    FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT)!!)
                }
        ))

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition (
                Function {
                    fsm.recvAck(it.idscpMessage.idscpAck)
                    FSM.FsmResult(FSM.FsmResultCode.OK, this)
                }
        ))

        setNoTransitionHandler (
                Function {
                    LOG.debug("No transition available for given event $it")
                    LOG.debug("Stay in state STATE_WAIT_FOR_RAT_VERIFIER")
                    FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
                }
        )
    }
}