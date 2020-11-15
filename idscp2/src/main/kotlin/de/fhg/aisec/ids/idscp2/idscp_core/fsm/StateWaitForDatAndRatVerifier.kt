package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.function.Function

/**
 * The Wait_For_Dat_And_Rat_Verifier State of the FSM of the IDSCP2 protocol.
 * Wait for a new dynamic attribute token from the peer, since the old one is not valid anymore
 * and waits for the RatVerifier after successful verification of the Dat to decide if the IDSCP2
 * connection will be established
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForDatAndRatVerifier(fsm: FSM,
                                    handshakeTimer: StaticTimer,
                                    datTimer: DynamicTimer,
                                    dapsDriver: DapsDriver) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER")
        LOG.debug("Set handshake timeout")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForDatAndRatVerifier::class.java)
    }

    init {

        /*---------------------------------------------------
         * STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER - Transition Description
         * ---------------------------------------------------
         * onICM: stop ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: error ---> {} ---> STATE_CLOSED
         * onICM: timeout ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_ACK ---> {cancel Ack flag} ---> STATE_WAIT_FOR_RAT
         * onMessage: IDSCP_CLOSE ---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT(success) --> {verify DAT, set det_timeout, start RAT_VERIFIER} ---> STATE_WAIT_FOR_RAT_VERIFEIER
         * onMessage: IDSCP_DAT(failed) --> {verify DAT, send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_DAT_EXPIRED ---> {send IDSCP_DAT, start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * onMessage: IDSCP_RE_RAT ---> {start IDSCP_PROVER} ---> STATE_WAIT_FOR_DAT_AND_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER
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
                fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("No valid DAT", CloseCause.NO_VALID_DAT))
                return@Function fsm.getState(FsmState.STATE_CLOSED)
            }
            LOG.debug("Remote DAT is valid. Set dat timeout")
            datTimer.resetTimeout(datValidityPeriod * 1000)
            //start RAT Verifier
            if (!fsm.restartRatVerifierDriver()) {
                LOG.error("Cannot run Rat verifier, close idscp connection")
                return@Function fsm.getState(FsmState.STATE_CLOSED)
            }
            fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER)
        }
        ))
        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition(
        Function {
            LOG.debug("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER")
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(dapsDriver.token))) {
                LOG.error("Cannot send DAT message")
                return@Function fsm.getState(FsmState.STATE_CLOSED)
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.error("Cannot run Rat prover, close idscp connection")
                return@Function fsm.getState(FsmState.STATE_CLOSED)
            }
            fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT)
        }
        ))
        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition(
        Function {
            LOG.debug("Received IDSCP_RE_RAT. Start RAT_PROVER")
            if (!fsm.restartRatProverDriver()) {
                LOG.error("Cannot run Rat prover, close idscp connection")
                return@Function fsm.getState(FsmState.STATE_CLOSED)
            }
            fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT)
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