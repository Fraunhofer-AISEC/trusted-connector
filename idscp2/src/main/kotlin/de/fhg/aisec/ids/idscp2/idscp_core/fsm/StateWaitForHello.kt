package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpClose.CloseCause
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.function.Function

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
                        private val handshakeTimer: Timer,
                        datTimer: Timer,
                        dapsDriver: DapsDriver,
                        localSupportedRatSuite: Array<String>,
                        localExpectedRatSuite: Array<String>) : State() {
    override fun runEntryCode(fsm: FSM) {
        LOG.debug("Switched to state STATE_WAIT_FOR_HELLO")
        LOG.debug("Set handshake timeout to 5 seconds")
        handshakeTimer.resetTimeout(5)
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
            LOG.debug("An internal control error occurred")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            LOG.debug("Received stop signal from user. Send IDSCP_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("User close",
                    CloseCause.USER_SHUTDOWN))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(InternalControlMessage.TIMEOUT.value, Transition {
            LOG.debug("STATE_WAIT_FOR_HELLO timeout. Send IDSCP_CLOSE")
            fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("Handshake Timeout",
                    CloseCause.TIMEOUT))
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(IdscpMessage.IDSCPCLOSE_FIELD_NUMBER, Transition {
            LOG.debug("Received IDSCP_CLOSE")
            fsm.getState(FsmState.STATE_CLOSED)
        })
        addTransition(IdscpMessage.IDSCPHELLO_FIELD_NUMBER, Transition(
                Function { event: Event ->
                    handshakeTimer.cancelTimeout()
                    val idscpHello = event.idscpMessage.idscpHello
                    LOG.debug("Received IDSCP_HELLO")
                    LOG.debug("Calculate Rat mechanisms")
                    val proverMechanism = fsm.getRatProverMechanism(localSupportedRatSuite,
                            idscpHello.expectedRatSuiteList.toTypedArray())
                    val verifierMechanism = fsm.getRatVerifierMechanism(localExpectedRatSuite,
                            idscpHello.supportedRatSuiteList.toTypedArray())
                    LOG.debug("Verify received DAT")
                    //check if Dat is available and verify dat
                    var datValidityPeriod: Long = 0
                    if (!idscpHello.hasDynamicAttributeToken() || 0 > dapsDriver
                                    .verifyToken(idscpHello.dynamicAttributeToken.token.toByteArray(), null)
                                    .also { datValidityPeriod = it }) {
                        LOG.debug("No valid remote DAT is available. Send IDSCP_CLOSE")
                        fsm.sendFromFSM(Idscp2MessageHelper.createIdscpCloseMessage("No valid DAT",
                                CloseCause.NO_VALID_DAT))
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    LOG.debug("Remote DAT is valid. Set dat timeout to its validity period")
                    datTimer.resetTimeout(datValidityPeriod)
                    fsm.setRatMechanisms(proverMechanism, verifierMechanism)
                    LOG.debug("Start RAT Prover and Verifier")
                    if (!fsm.restartRatVerifierDriver()) {
                        LOG.error("Cannot run Rat verifier, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    if (!fsm.restartRatProverDriver()) {
                        LOG.error("Cannot run Rat prover, close idscp connection")
                        return@Function fsm.getState(FsmState.STATE_CLOSED)
                    }
                    fsm.getState(FsmState.STATE_WAIT_FOR_RAT)
                }
        ))
        setNoTransitionHandler { event: Event? ->
            LOG.debug("No transition available for given event " + event.toString())
            LOG.debug("Stay in state STATE_WAIT_FOR_HELLO")
            this
        }
    }
}