package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory

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
        if (LOG.isTraceEnabled) {
            LOG.trace("Set handshake timeout")
        }
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
            //toDo security requirements
            val dat = event.idscpMessage.idscpDat.token.toByteArray()
            var datValidityPeriod: Long

            try {
                if (0 > dapsDriver.verifyToken(dat, null).also { datValidityPeriod = it }) {
                    LOG.warn("No valid remote DAT is available. Send IDSCP_CLOSE")
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

            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT_VERIFIER))
        })

        addTransition(IdscpMessage.IDSCPDATEXPIRED_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_DAT_EXPIRED. Send new DAT from DAT_DRIVER, start RAT_PROVER")
            }
            if (!fsm.sendFromFSM(Idscp2MessageHelper.createIdscpDatMessage(fsm.getDynamicAttributeToken))) {
                LOG.warn("Cannot send DAT message")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT))
        })

        addTransition(IdscpMessage.IDSCPRERAT_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_RE_RAT. Start RAT_PROVER")
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_DAT_AND_RAT))
        })

        addTransition(IdscpMessage.IDSCPACK_FIELD_NUMBER, Transition {
            fsm.recvAck(it.idscpMessage.idscpAck)
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        setNoTransitionHandler { event: Event? ->
            if (LOG.isTraceEnabled) {
                LOG.trace("No transition available for given event " + event.toString())
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }

    }
}