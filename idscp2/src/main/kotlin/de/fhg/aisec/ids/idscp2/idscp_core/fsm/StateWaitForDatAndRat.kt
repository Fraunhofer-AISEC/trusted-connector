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
 * The Wait_For_Dat_And_Rat State of the FSM of the IDSCP2 protocol.
 * Waits for a new valid dynamic attribute token from the peer as well as for the RatProver and
 * RatVerifier to decide whether the connection will be established or not
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForDatAndRat(fsm: FSM,
                            handshakeTimer: Timer,
                            proverHandshakeTimer: Timer,
                            datTimer: Timer,
                            dapsDriver: DapsDriver
) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_DAT_AND_RAT")
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
            LOG.debug("Send IDSC_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close",
                    CloseCause.USER_SHUTDOWN))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.debug("An internal control error occurred")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.TIMEOUT.value, Transition {
            LOG.debug("Handshake timeout occurred. Send IDSCP_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("Handshake timeout",
                    CloseCause.TIMEOUT))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.RAT_PROVER_OK.value, Transition {
            LOG.debug("Received RAT_PROVER OK")
            proverHandshakeTimer.cancelTimeout()
            fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER)
        })
        addTransition(InternalControlMessage.RAT_PROVER_FAILED.value, Transition {
            LOG.error("RAT_PROVER failed")
            LOG.debug("Send IDSC_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("RAT_PROVER failed",
                    CloseCause.RAT_PROVER_FAILED))
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
        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            LOG.debug("Received IDSCP_CLOSE")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(IdscpMessage.IDSCPDAT_FIELD_NUMBER, Transition(
                Function { event: Event ->
                    handshakeTimer.cancelTimeout()
                    LOG.debug("Verify received DAT")
                    //check if Dat is available and verify dat
                    val dat = event.idscpMessage.idscpDat.token.toByteArray()
                    var datValidityPeriod: Long
                    if (0 > dapsDriver.verifyToken(dat, null).also { datValidityPeriod = it }) {
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE")
                        fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage(
                                "No valid DAT", CloseCause.NO_VALID_DAT))
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    LOG.debug("Remote DAT is valid. Set dat timeout")
                    datTimer.resetTimeout(datValidityPeriod)
                    //start RAT Verifier
                    if (!fsm.restartRatVerifierDriver()) {
                        LOG.error("Cannot run Rat verifier, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT)
                }
        ))
        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition(
                Function {
                    LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, restart RAT_PROVER")
                    if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(dapsDriver.token))) {
                        LOG.error("Cannot send Dat message")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
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
                    if (fsm.getAckFlag) {
                        LOG.debug("Received IdscpAck, cancel flag in fsm")
                        fsm.setAckFlag(false)
                    } else {
                        LOG.warn("Received unexpected IdscpAck")
                    }
                    this
                }
        ))
        setNoTransitionHandler { event: Event? ->
            LOG.debug("No transition available for given event " + event.toString())
            this
        }
    }
}