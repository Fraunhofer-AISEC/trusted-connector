package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.function.Function
import java.util.stream.Collectors

/**
 * The Closed State of the FSM of the IDSCP2 protocol.
 * The FSM is in the Closed state either before any transition was triggered (in this case, the
 * Closed State is the FSM Start state) or after the connection was closed (in this case, the
 * Closed State is the FSM final state without any outgoing transitions)
 *
 *
 * When the FSM go from any State into the Closed State again, the FSM is locked forever and all
 * involved actors like RatDrivers and Timers will be terminated
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
internal class StateClosed(fsm: FSM,
                           dapsDriver: DapsDriver,
                           onMessageLock: Condition,
                           attestationConfig: AttestationConfig) : State() {

    private fun runExitCode(onMessageLock: Condition) {
        //State Closed exit code
        onMessageLock.signalAll() //enables fsm.onMessage()
    }

    override fun runEntryCode(fsm: FSM) {
        //State Closed entry code
        LOG.debug("Switched to state STATE_CLOSED")
        fsm.shutdownFsm()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(StateClosed::class.java)
    }

    init {


        /*---------------------------------------------------
         * STATE_CLOSED - Transition Description
         * ---------------------------------------------------
         * onICM: start_handshake --> {send IDSCP_HELLO, set handshake_timeout} --> STATE_WAIT_FOR_HELLO
         * ALL_OTHER_MESSAGES ---> STATE_CLOSED
         * --------------------------------------------------- */
        addTransition(InternalControlMessage.START_IDSCP_HANDSHAKE.value, Transition(
            Function {
                LOG.debug("Get DAT Token vom DAT_DRIVER")
                val dat = dapsDriver.token
                LOG.debug("Send IDSCP_HELLO")
                val idscpHello = Idscp2MessageHelper.createIdscpHelloMessage(
                        dat,
                        attestationConfig.supportedAttestationSuite,
                        attestationConfig.expectedAttestationSuite
                )
                if (!fsm.sendFromFSM(idscpHello)) {
                    LOG.error("Cannot send IdscpHello. Close connection")
                    runEntryCode(fsm)
                    onMessageLock.signalAll()
                    return@Function fsm.getState(FsmState.STATE_CLOSED)
                }
                runExitCode(onMessageLock)
                fsm.getState(FsmState.STATE_WAIT_FOR_HELLO)
            }
        ))

        // This origins from onClose(), so just ignore it
        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition (
                Function {
                    if (LOG.isTraceEnabled) {
                        LOG.trace("Received STOP in STATE_CLOSED, ignored.")
                    }
                    this
                }
        ))

        setNoTransitionHandler(
                Function {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("No transition available for given event {}, stack trace for analysis:\n{}",
                                it,
                                Arrays.stream(Thread.currentThread().stackTrace)
                                        .skip(1)
                                        .map { obj: StackTraceElement -> obj.toString() }
                                        .collect(Collectors.joining("\n")))
                        LOG.debug("Stay in state STATE_CLOSED")
                    }
                    this
                }
        )
    }
}