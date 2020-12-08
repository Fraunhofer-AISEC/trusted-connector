package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import de.fhg.aisec.ids.idscp2.idscp_core.api.configuration.AttestationConfig
import de.fhg.aisec.ids.idscp2.idscp_core.fsm.FSM.FsmState
import de.fhg.aisec.ids.idscp2.idscp_core.messages.Idscp2MessageHelper
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.Condition
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
                           onMessageLock: Condition,
                           attestationConfig: AttestationConfig) : State() {

    private fun runExitCode(onMessageLock: Condition) {
        //State Closed exit code
        onMessageLock.signalAll() //enables fsm.onMessage()
    }

    override fun runEntryCode(fsm: FSM) {
        //State Closed entry code
        if (LOG.isTraceEnabled) {
            LOG.trace("Switched to state STATE_CLOSED")
        }
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
        addTransition(InternalControlMessage.START_IDSCP_HANDSHAKE.value, Transition {
            if (fsm.isFsmLocked) {
                if (LOG.isTraceEnabled) {
                    LOG.trace("Cannot start handshake, because FSM is locked forever. Ignored.")
                }
                return@Transition FSM.FsmResult(FSM.FsmResultCode.FSM_LOCKED, this)
            }

            // FSM not locked, start handshake
            LOG.debug("Get DAT Token vom DAT_DRIVER")
            val dat = fsm.getDynamicAttributeToken
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
                return@Transition FSM.FsmResult(FSM.FsmResultCode.IO_ERROR, this)
            }
            runExitCode(onMessageLock)
            FSM.FsmResult(FSM.FsmResultCode.OK, fsm.getState(FsmState.STATE_WAIT_FOR_HELLO))
        })


        addTransition(InternalControlMessage.REPEAT_RAT.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received RepeatRat in STATE_CLOSED, ignored.")
            }

            // return either FSM_LOCKED or FSM_NOT_STARTED
            if (fsm.isFsmLocked) {
                FSM.FsmResult(FSM.FsmResultCode.FSM_LOCKED, this)
            } else {
                FSM.FsmResult(FSM.FsmResultCode.FSM_NOT_STARTED, this)
            }
        })

        addTransition(InternalControlMessage.SEND_DATA.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received SEND in STATE_CLOSED, ignored.")
            }

            // return either FSM_LOCKED or FSM_NOT_STARTED
            if (fsm.isFsmLocked) {
                FSM.FsmResult(FSM.FsmResultCode.FSM_LOCKED, this)
            } else {
                FSM.FsmResult(FSM.FsmResultCode.FSM_NOT_STARTED, this)
            }
        })

        addTransition(InternalControlMessage.IDSCP_STOP.value, Transition {
            if (LOG.isTraceEnabled) {
                LOG.trace("Received STOP in STATE_CLOSED, ignored.")
            }

            // return either FSM_LOCKED or FSM_NOT_STARTED
            if (fsm.isFsmLocked) {
                FSM.FsmResult(FSM.FsmResultCode.FSM_LOCKED, this)
            } else {
                FSM.FsmResult(FSM.FsmResultCode.FSM_NOT_STARTED, this)
            }
        })

        setNoTransitionHandler {
            if (LOG.isDebugEnabled) {
                LOG.debug("No transition available for given event {}, stack trace for analysis:\n{}",
                    it,
                    Arrays.stream(Thread.currentThread().stackTrace)
                        .skip(1)
                        .map { obj: StackTraceElement -> obj.toString() }
                        .collect(Collectors.joining("\n")))
                LOG.debug("Stay in state STATE_CLOSED")
            }
            FSM.FsmResult(FSM.FsmResultCode.UNKNOWN_TRANSITION, this)
        }
    }
}