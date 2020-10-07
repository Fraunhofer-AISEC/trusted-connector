package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * The Established State of the FSM of the IDSCP2 protocol.
 * Allows message exchange over the IDSCP2 protocol between two connectors
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForAck(fsm: FSM,
                       dapsDriver: DapsDriver,
                       ratTimer: Timer,
                       handshakeTimer: Timer,
                       ackTimer: Timer) : State() {

    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_ACK")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForAck::class.java)
    }

    init {


        /*---------------------------------------------------
         * STATE_ESTABLISHED - Transition Description
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
            LOG.debug("Error occurred, close idscp connection")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            LOG.debug("Send IDSCP_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close",
                    CloseCause.USER_SHUTDOWN))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition(
                Function {
                    LOG.debug("Request RAT repeat. Send IDSCP_RERAT, start RAT_VERIFIER")
                    ratTimer.cancelTimeout()
                    ackTimer.cancelTimeout()
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpReRatMessage(""))) {
                        LOG.error("Cannot send ReRat message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    if (!fsm.restartRatVerifierDriver()) {
                        LOG.error("Cannot run Rat verifier, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER)
                }
        ))
        addTransition(InternalControlMessage.SEND_DATA.value, Transition {
            LOG.warn("Cannot send data in STATE_WAIT_FOR_ACK")
            this
        })
        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition(
                Function {
                    ratTimer.cancelTimeout()
                    ackTimer.cancelTimeout()
                    LOG.debug("Remote DAT expired. Send IDSCP_DAT_EXPIRED")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                        LOG.error("Cannot send DatExpired message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    LOG.debug("Set handshake timeout")
                    handshakeTimer.resetTimeout(5)
                    fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER)
                }
        ))
        addTransition(InternalControlMessage.ACK_TIMER_EXPIRED.value, Transition(
                Function {
                    LOG.debug("ACK_timeout occurred. Send buffered IdscpData again")
                    if (fsm.getBufferedIdscpMessage != null || fsm.sendFromFSM(fsm.getBufferedIdscpMessage)) {
                        ackTimer.start(1)
                        return@Function fsm.getState(FsmState.STATE_WAIT_FOR_ACK)
                    } else {
                        LOG.warn("Cannot send IdscpData, shutdown FSM")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                }
        ))
        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_RERAT. Start RAT_PROVER")
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    ackTimer.cancelTimeout()
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER)
                }
        ))
        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("DAT expired. Send new DAT and repeat RAT")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(dapsDriver.token))) {
                        LOG.error("Cannot send Dat message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    ackTimer.cancelTimeout()
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER)
                }
        ))
        addTransition(IdscpMessage.IDSCPDATA_FIELD_NUMBER, Transition { event: Event ->
            // send Idscp Ack
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpAckMessage())) {
                LOG.warn("Cannot send ACK")
            }
            val data = event.idscpMessage.idscpData
            fsm.notifyIdscpMsgListener(data.data.toByteArray())
            this
        })
        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition {
            LOG.debug("Received IdscpAck, cancel AckFlag")
            ackTimer.cancelTimeout()
            fsm.setAckFlag(false)
            fsm.getState(FsmState.STATE_ESTABLISHED)
        })
        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            LOG.debug("Receive IDSCP_CLOSED")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        setNoTransitionHandler { event: Event? ->
            LOG.debug("No transition available for given event " + event.toString())
            LOG.debug("Stay in state STATE_WAIT_FOR_ACK")
            this
        }
    }
}