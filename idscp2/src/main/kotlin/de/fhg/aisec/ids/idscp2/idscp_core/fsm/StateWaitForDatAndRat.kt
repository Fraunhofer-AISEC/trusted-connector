package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * The Wait_For_Dat_And_Rat State of the FSM of the IDSCP2 protocol.
 * Waits for a new valid dynamic attribute token from the peer as well as for the RatProver and
 * RatVerifier to decide whether the connection will be established or not
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForDatAndRat(fsm: FSM,
                            handshakeTimer: StaticTimer,
                            proverHandshakeTimer: StaticTimer,
                            datTimer: DynamicTimer,
                            dapsDriver: DapsDriver
) : State() {
    override fun runEntryCode(fsm: FSM) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_WAIT_FOR_DAT_AND_RAT")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForDatAndRat::class.java)
    }

    init {

        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: error ---> {stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onICM: rat_prover_ok ---> {} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onICM: rat_prover_failed ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: rat_prover_msg ---> {send IDSCP_RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_ACK ---> {cancel Ack flag} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {ratP.stop(), timeouts.stop()} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT(success) ---> {verify dat, start dat_timeout, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_DAT(failed) ---> {verify dat, send IDSCP_CLOSE, stop RAT_PROVER} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, restart RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RAT_VERIFIER ---> {delegate to RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RE_RAT ---> {restart RAT_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_ESTABLISHED
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Send IDSC_CLOSE")
            }
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "User close",
                    CloseCause.USER_SHUTDOWN
                )
            )
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.warn("An internal control error occurred")
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
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

        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received RAT_PROVER OK")
            }
            proverHandshakeTimer.cancelTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER))
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

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_CLOSE")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(IdscpMessage.IDSCPDAT_FIELD_NUMBER, Transition { event: Event ->

            handshakeTimer.cancelTimeout()
            if (LOG.isTraceEnabled) {
                LOG.trace("Verify received DAT")
            }

            //check if Dat is available and verify dat
            val dat = event.idscpMessage.idscpDat.token.toByteArray()
            var datValidityPeriod: Long

            try {
                if (0 > dapsDriver.verifyToken(dat).also { datValidityPeriod = it }) {
                    if (LOG.isTraceEnabled) {
                        LOG.trace("No valid remote DAT is available. Send IDSCP_CLOSE")
                    }
                    fsm.sendFromFSM(
                        Idscp2MessageHelper.createIdscpCloseMessage(
                            "No valid DAT", CloseCause.NO_VALID_DAT
                        )
                    )
                    return@Transition FSM.FsmResult(
                        FSM.FsmResultCode.INVALID_DAT,
                        fsm.getState(FsmState.STATE_CLOSED)
                    )
                }
            } catch (e: Exception) {
                LOG.warn("DapsDriver has thrown Exception while validating remote DAT. Send IDSCP_CLOSE: {}", e)
                fsm.sendFromFSM(
                    Idscp2MessageHelper.createIdscpCloseMessage(
                        "No valid DAT", CloseCause.NO_VALID_DAT
                    )
                )
                return@Transition FSM.FsmResult(FSM.FsmResultCode.INVALID_DAT, fsm.getState(FsmState.STATE_CLOSED))
            }

            if (LOG.isTraceEnabled) {
                LOG.trace("Remote DAT is valid. Set dat timeout")
            }
            datTimer.resetTimeout(datValidityPeriod * 1000)

            //start RAT Verifier
            if (!fsm.restartRatVerifierDriver()) {
                LOG.warn("Cannot run Rat verifier, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT))
        })

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(fsm.getDynamicAttributeToken))) {
                LOG.warn("Cannot send Dat message")
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
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }
    }
}