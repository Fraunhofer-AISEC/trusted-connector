package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory

/**
 * The Established State of the FSM of the IDSCP2 protocol.
 * Allows message exchange over the IDSCP2 protocol between two connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForAck(fsm: FSM,
                      ratTimer: StaticTimer,
                      handshakeTimer: StaticTimer,
                      ackTimer: StaticTimer) : State() {

    override fun runEntryCode(fsm: FSM) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_WAIT_FOR_ACK")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForAck::class.java)
    }

    init {


        /*---------------------------------------------------
         * STATW_WAIT_FOR_ACK - Transition Description
         * ---------------------------------------------------
         * onICM: error ---> {timeouts.cancel(), send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: stop ---> {timeouts.cancel()} ---> STATE_CLOSED
         * onICM: re_rat ---> {send IDSCP_RE_RAT, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFIER
         * onICM: send_data ---> {send IDS_DATA} ---> STATE_WAIT_FOR_ACK
         * onICM: dat_timeout ---> {send IDSCP_DAT_EXPIRED} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
         * onICM. ackTimeout --> {send IDSCP_DATA again} --> STATE_WAIT_FOR_ACK
         * onMessage: IDSCP_DATA ---> {delegate to connection} ---> STATE_WAIT_FOR_ACK
         * onMessage: IDSCP_ACK ---> {clear ACK flag} ---> STATE_ESTABLISHED
         * onMessage: IDSCP_RERAT ---> {start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start RAT_PROVER} ---> STATE_WAIT_FOR_RAT_PROVER
         * onMessage: IDSCP_CLOSE ---> {timeouts.cancel()} ---> STATE_CLOSED
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_ESTABLISHED
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.warn("Error occurred, close idscp connection")
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

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Request RAT repeat. Send IDSCP_RERAT, start RAT_VERIFIER")
            }
            ratTimer.cancelTimeout()
            ackTimer.cancelTimeout()
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpReRatMessage(""))) {
                LOG.warn("Cannot send ReRat message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (!fsm.restartRatVerifierDriver()) {
                LOG.warn("Cannot run Rat verifier, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER))
        })

        addTransition(InternalControlMessage.SEND_DATA.value, Transition {
            LOG.warn("Cannot send data in STATE_WAIT_FOR_ACK")
            FSM.FsmResult(FSM.FsmResultCode.WOULD_BLOCK, this)
        })

        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition {
            ratTimer.cancelTimeout()
            ackTimer.cancelTimeout()
            if (LOG.isTraceEnabled) {
                LOG.trace("Remote DAT expired. Send IDSCP_DAT_EXPIRED")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                LOG.warn("Cannot send DatExpired message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (LOG.isTraceEnabled) {
                LOG.trace("Set handshake timeout")
            }
            handshakeTimer.resetTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER))
        })

        addTransition(InternalControlMessage.ACK_TIMER_EXPIRED.value, Transition {
            if (fsm.ackFlag) {
                fsm.getBufferedIdscpMessage?.let {
                    if (LOG.isTraceEnabled) {
                        LOG.trace("ACK_timeout occurred. Sending buffered IdscpData again...")
                    }
                    return@Transition if (!fsm.sendFromFSM(it)) {
                        LOG.warn("Cannot send IdscpData, shutdown FSM")
                        FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
                    } else {
                        ackTimer.start()
                        FSM.FsmResult(FSM.FsmResultCode.OK, this)
                    }
                }
            }
            LOG.warn("No IdscpData message buffered in state WAIT_FOR_ACK. Shutdown")
            return@Transition FSM.FsmResult(
                FSM.FsmResultCode.IDSCP_DATA_NOT_CACHED,
                fsm.getState(FsmState.STATE_CLOSED)
            )
        })

        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_RERAT. Start RAT_PROVER")
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            ackTimer.cancelTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER))
        })

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("DAT expired. Send new DAT and repeat RAT")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(fsm.getDynamicAttributeToken))) {
                LOG.warn("Cannot send Dat message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            ackTimer.cancelTimeout()
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER))
        })

        addTransition(IdscpMessage.IDSCPDATA_FIELD_NUMBER, Transition {
            fsm.recvData(it.idscpMessage.idscpData)
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition {
            if (fsm.recvAck(it.idscpMessage.idscpAck)) {
                FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_ESTABLISHED))
            } else {
                FSM.FsmResult(FSM.FsmResultCode.OK, this)
            }
        })

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Receive IDSCP_CLOSED")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        setNoTransitionHandler {
            if (LOG.isTraceEnabled) {
                LOG.trace("No transition available for given event $it")
                LOG.trace("Stay in state STATE_WAIT_FOR_ACK")
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }

    }
}