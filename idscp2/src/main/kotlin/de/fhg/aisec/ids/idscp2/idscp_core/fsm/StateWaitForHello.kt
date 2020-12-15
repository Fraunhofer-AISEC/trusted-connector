package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory

/**
 * The Wait_For_Hello State of the FSM of the IDSCP2 protocol.
 * Waits for the Idscpv2 Hellp Message that contains the protocol version, the supported and
 * expected remote attestation cipher suites and the dynamic attribute token (DAT) of the peer.
 *
 *
 * Goes into the WAIT_FOR_RAT State when valid Rat mechanisms were found and the DAT is valid
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class StateWaitForHello(fsm: FSM,
                        private val handshakeTimer: StaticTimer,
                        datTimer: DynamicTimer,
                        dapsDriver: DapsDriver,
                        attestationConfig: AttestationConfig) : State() {
    override fun runEntryCode(fsm: FSM) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_WAIT_FOR_HELLO")
            LOG.trace("Set handshake timeout to 5 seconds")
        }
        handshakeTimer.resetTimeout()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateWaitForHello::class.java)
    }

    init {

        /*---------------------------------------------------
         * STATE_WAIT_FOR_HELLO - Transition Description
         * ---------------------------------------------------
         * onICM: error --> {} ---> STATE_CLOSED
         * onICM: stop --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onICM: timeout --> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_CLOSE---> {} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (no rat match) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (invalid DAT) ---> {send IDSCP_CLOSE} ---> STATE_CLOSED
         * onMessage: IDSCP_HELLO (SUCCESS) ---> {verify DAT, match RAT, set DAT Timeout, start RAT P&V,
         *                                        set handshake_timeout} ---> STATE_WAIT_FOR_RAT
         * ALL_OTHER_MESSAGES ---> {} ---> STATE_WAIT_FOR_HELLO
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.ERROR.value, Transition {
            LOG.warn("An internal control error occurred")
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received stop signal from user. Send IDSCP_CLOSE")
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
            if (LOG.isTraceEnabled) {
                LOG.trace("Received SEND signal from user, but FSM is not connected yet")
            }
            FSM.FsmResult(FSM.FsmResultCode.NOT_CONNECTED, this)
        })

        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            // nothing to to, result should be okay since RAT will be done in the next
            // state for the first time
            if (LOG.isTraceEnabled) {
                LOG.trace("Received REPEAT_RAT signal from user")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, this)
        })

        addTransition(InternalControlMessage.TIMEOUT.value, Transition {
            LOG.warn("STATE_WAIT_FOR_HELLO timeout. Send IDSCP_CLOSE")
            fsm.sendFromFSM(
                Idscp2MessageHelper.createIdscpCloseMessage(
                    "Handshake Timeout",
                    CloseCause.TIMEOUT
                )
            )
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_CLOSE. Close connection")
            }
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_CLOSED))
        })

        addTransition(IdscpMessage.IDSCPHELLO_FIELD_NUMBER, Transition { event: Event ->
            handshakeTimer.cancelTimeout()

            val idscpHello = event.idscpMessage.idscpHello
            if (LOG.isTraceEnabled) {
                LOG.trace("Received IDSCP_HELLO")
            }

            // toDo compare certificate fingerprints
            // toDo check for valid calculation of rat mechanisms, return NegotiationError
            if (LOG.isTraceEnabled) {
                LOG.trace("Calculate Rat mechanisms")
            }
            val proverMechanism = fsm.getRatProverMechanism(
                attestationConfig.supportedAttestationSuite,
                idscpHello.expectedRatSuiteList.toTypedArray()
            )
            val verifierMechanism = fsm.getRatVerifierMechanism(
                attestationConfig.expectedAttestationSuite,
                idscpHello.supportedRatSuiteList.toTypedArray()
            )

            // toDo securityRequirements
            if (LOG.isTraceEnabled) {
                LOG.trace("Verify received DAT")
            }
            //check if Dat is available and verify dat
            var datValidityPeriod: Long

            if (!idscpHello.hasDynamicAttributeToken()) {
                LOG.warn("No remote DAT is available. Send IDSCP_CLOSE")
                fsm.sendFromFSM(
                    Idscp2MessageHelper.createIdscpCloseMessage(
                        "No valid DAT", CloseCause.NO_VALID_DAT
                    )
                )
                return@Transition FSM.FsmResult(FSM.FsmResultCode.MISSING_DAT, fsm.getState(FsmState.STATE_CLOSED))
            }

            try {
                if (0 > dapsDriver.verifyToken(idscpHello.dynamicAttributeToken.token.toByteArray(), null)
                        .also { datValidityPeriod = it }
                ) {
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
                LOG.warn("DapsDriver throws Exception while validating remote DAT. Send IDSCP_CLOSE {}", e)
                fsm.sendFromFSM(
                    Idscp2MessageHelper.createIdscpCloseMessage(
                        "No valid DAT", CloseCause.NO_VALID_DAT
                    )
                )
                return@Transition FSM.FsmResult(FSM.FsmResultCode.INVALID_DAT, fsm.getState(FsmState.STATE_CLOSED))
            }

            if (LOG.isTraceEnabled) {
                LOG.trace("Remote DAT is valid. Set dat timeout to its validity period")
            }
            datTimer.resetTimeout(datValidityPeriod * 1000)
            fsm.setRatMechanisms(proverMechanism, verifierMechanism)

            if (LOG.isTraceEnabled) {
                LOG.debug("Start RAT Prover and Verifier")
            }
            if (!fsm.restartRatVerifierDriver()) {
                LOG.warn("Cannot run Rat verifier, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }
            if (!fsm.restartRatProverDriver()) {
                LOG.warn("Cannot run Rat prover, close idscp connection")
                return@Transition FSM.FsmResult(FSM.FsmResultCode.RAT_ERROR, fsm.getState(FsmState.STATE_CLOSED))
            }

            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_RAT))
        })

        setNoTransitionHandler {
            if (LOG.isTraceEnabled) {
                LOG.trace("No transition available for given event $it")
                LOG.trace("Stay in state STATE_WAIT_FOR_HELLO")
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }
    }
}