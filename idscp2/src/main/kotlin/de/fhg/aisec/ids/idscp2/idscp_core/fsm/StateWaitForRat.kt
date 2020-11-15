package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.function.Function

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
                      ratTimer: StaticTimer,
                      dapsDriver: DapsDriver) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switch to state STATE_WAIT_FOR_RAT")
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
            LOG.debug("An internal control error occurred")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            LOG.debug("Send IDSC_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close",
                    CloseCause.USER_SHUTDOWN))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition {
            LOG.debug("Received RAT_PROVER OK")
            proverHandshakeTimer.cancelTimeout()
            fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER)
        })
        addTransition(InternalControlMessage.RAT_VERIFIER_OK.value, Transition {
            LOG.debug("Received RAT_VERIFIER OK")
            verifierHandshakeTimer.cancelTimeout()
            ratTimer.resetTimeout()
            fsm.getState(FsmState.STATE_WAIT_FOR_RAT_PROVER)
        })
        addTransition(InternalControlMessage.RAT_PROVER_FAILED.value, Transition {
            LOG.error("RAT_PROVER failed")
            LOG.debug("Send IDSC_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("RAT_PROVER failed",
                    CloseCause.RAT_PROVER_FAILED))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.RAT_VERIFIER_FAILED.value, Transition {
            LOG.error("RAT_VERIFIER failed")
            LOG.debug("Send IDSC_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("RAT_VERIFIER failed",
                    CloseCause.RAT_VERIFIER_FAILED))
            fsm.getState(FsmState.STATE_CLOSED)
        })
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
        addTransition(InternalControlMessage.RAT_VERIFIER_MSG.value, Transition(
                Function { event: Event ->
                    LOG.debug("Send IDSCP_RAT_VERIFIER")
                    if (!fsm.sendFromFSM(event.idscpMessage)) {
                        LOG.error("Cannot send rat verifier message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    this
                }
        ))
        addTransition(InternalControlMessage.DAT_TIMER_EXPIRED.value, Transition(
                Function {
                    LOG.debug("DAT timeout, send IDSCP_DAT_EXPIRED and cancel RAT_VERIFIER")
                    fsm.stopRatVerifierDriver()
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatExpiredMessage())) {
                        LOG.error("Cannot send DatExpired message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    LOG.debug("Start Handshake Timer")
                    handshakeTimer.resetTimeout()
                    fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT)
                }
        ))
        addTransition(InternalControlMessage.TIMEOUT.value, Transition {
            LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("Handshake timeout",
                    CloseCause.TIMEOUT))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition (
                Function {
                    fsm.recvAck(it.idscpMessage.idscpAck)
                    this
                }
        ))
        addTransition(IdscpMessage.IDSCPRATVERIFIER_FIELD_NUMBER, Transition { event: Event ->
            LOG.debug("Delegate received IDSCP_RAT_VERIFIER to RAT_PROVER")
            assert(event.idscpMessage.hasIdscpRatVerifier())
            fsm.ratProverDriver?.delegate(event.idscpMessage.idscpRatVerifier.data.toByteArray())
                    ?: throw Idscp2Exception("RAT prover driver not available")
            this
        })
        addTransition(IdscpMessage.IDSCPRATPROVER_FIELD_NUMBER, Transition { event: Event ->
            LOG.debug("Delegate received IDSCP_RAT_PROVER to RAT_VERIFIER")
            assert(event.idscpMessage.hasIdscpRatProver())
            fsm.ratVerifierDriver?.delegate(event.idscpMessage.idscpRatProver.data.toByteArray())
                    ?: throw Idscp2Exception("RAT verifier driver not available")
            this
        })
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
        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            LOG.debug("Received IDSCP_CLOSE")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        setNoTransitionHandler { event: Event? ->
            LOG.debug("No transition available for given event " + event.toString())
            LOG.debug("Stay in state STATE_WAIT_FOR_RAT")
            this
        }
    }
}