package de.fhg.aisec.ids.idscp2.idscp_core.fsm

import com.google.protobuf.InvalidProtocolBufferException
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatProverDriver
import de.fhg.aisec.ids.idscp2.drivers.interfaces.RatVerifierDriver
import de.fhg.aisec.ids.idscp2.error.Idscp2Exception
import de.fhg.aisec.ids.idscp2.idscp_core.FastLatch
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2Connection
import de.fhg.aisec.ids.idscp2.idscp_core.Idscp2MessageHelper
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.rat_registry.RatVerifierDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.secure_channel.SecureChannel
import de.fhg.aisec.ids.idscp2.messages.IDSCP2.IdscpMessage
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * The finite state machine FSM of the IDSCP2 protocol
 *
 *
 * Manages IDSCP2 Handshake, Re-Attestation, DAT-ReRequest and DAT-Re-Validation. Delivers
 * Internal Control Messages and Idscpv2Messages to the target receivers,
 * creates and manages the states and its transitions and implements security restriction to protect
 * the protocol against misuse and faulty, insecure or evil driver implementations.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class FSM(connection: Idscp2Connection, secureChannel: SecureChannel, dapsDriver: DapsDriver,
          localSupportedRatSuite: Array<String>, localExpectedRatSuite: Array<String>, ratTimeout: Long) : FsmListener {
    /*  -----------   IDSCP2 Protocol States   ---------- */
    private val states = HashMap<FsmState, State>()

    enum class FsmState {
        STATE_CLOSED, STATE_WAIT_FOR_HELLO, STATE_WAIT_FOR_RAT, STATE_WAIT_FOR_RAT_VERIFIER, STATE_WAIT_FOR_RAT_PROVER, STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER, STATE_WAIT_FOR_DAT_AND_RAT, STATE_ESTABLISHED
    }

    private var currentState: State?

    /*  ----------------   end of states   --------------- */
    private val connection: Idscp2Connection
    private val secureChannel: SecureChannel
    var ratProverDriver: RatProverDriver<*>? = null
        private set
    var ratVerifierDriver: RatVerifierDriver<*>? = null
        private set

    /**
     * RAT Driver Thread ID to identify the driver threads and check if messages are provided by
     * the current active driver or by any old driver, whose lifetime is already over
     *
     * Only one driver can be valid at a time
     */
    private var currentRatProverId //avoid messages from old prover drivers
            : String? = null
    private var currentRatVerifierId //avoid messages from old verifier drivers
            : String? = null

    /**
     * RAT Mechanisms, calculated during handshake in WAIT_FOR_HELLO_STATE
     */
    private var proverMechanism: String? = null //RAT prover mechanism
    private var verifierMechanism: String? = null //RAT Verifier mechanism

    /**
     * A FIFO-fair synchronization lock for the finite state machine
     */
    private val fsmIsBusy = ReentrantLock(true)

    /**
     * A condition to ensure no idscp messages can be provided by the secure channel to the fsm
     * before the handshake was started
     */
    private val onMessageBlock = fsmIsBusy.newCondition()

    /**
     * A condition for the dscpv2 handshake to wait until the handshake was successful and the
     * connection is established or the handshake failed and the fsm is locked forever
     */
    private val idscpHandshakeLock = fsmIsBusy.newCondition()
    private var handshakeResultAvailable = false
    private val idscpHandshakeCompletedLatch = FastLatch()

    /**
     * Check if FSM is closed forever
     */
    private var fsmTerminated = false

    /* ---------------------- Timer ---------------------- */
    private val datTimer: Timer
    private val ratTimer: Timer
    private val handshakeTimer: Timer
    private val proverHandshakeTimer: Timer
    private val verifierHandshakeTimer: Timer
    private fun checkForFsmCycles() {
        // check if current thread holds already the fsm lock, then we have a circle
        // this runs into an issue: onControlMessage must be called only from other threads!
        // if the current thread currently stuck within a fsm transition it will trigger another
        // transition on the old state and undefined behaviour occurred
        //
        // The IDSCP2 core and default driver will not run into this issue. It's a protection for
        // avoiding incorrect usage of the IDSCP2 library from further driver implementations
        //
        // Example:
        // Thread A stuck within a transition t1 that calls a function that calls
        // onControlMessage(InternalError). Then the error is handled in the current
        // state and the fsm will switch into state STATE_CLOSED and close all resources.
        //
        // Afterwards, the thread will continue the old transition t1 that might use some of the
        // closed resources and switch in a non-closed STATE, e.g. STATE_ESTABLISHED.
        // So our fsm would be broken and the behaviour is undefined and could leak security
        // vulnerabilities
        //
        if (fsmIsBusy.isHeldByCurrentThread) {
            val e = RuntimeException("The current thread holds the fsm lock already. "
                    + "A circle might occur that could lead to undefined behaviour within the fsm")
            // Log exception before throwing, since some threads swallow the exception without any notification
            LOG.error(e.message, e)
            throw e
        }
    }

    /**
     * Get a new IDSCP2 Message from the secure channel and provide it as an event to the fsm
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     */
    override fun onMessage(data: ByteArray) {

        //check for incorrect usage
        checkForFsmCycles()

        //parse message and create new IDSCP Message event, then pass it to current state and
        // update new state
        val message: IdscpMessage = try {
            IdscpMessage.parseFrom(data)
        } catch (e: InvalidProtocolBufferException) {
            LOG.warn("Cannot parse raw data into IdscpMessage {}", data)
            return
        }
        val event = Event(message)
        //must wait when fsm is in state STATE_CLOSED --> wait() will be notified when fsm is
        // leaving STATE_CLOSED
        fsmIsBusy.lock()
        try {
            while (currentState == states[FsmState.STATE_CLOSED]) {
                if (fsmTerminated) {
                    return
                }
                try {
                    onMessageBlock.await()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            feedEvent(event)
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /**
     * An internal control message (ICM) occurred, provide it to the fsm as an event
     */
    private fun onControlMessage(controlMessage: InternalControlMessage) {
        //create Internal Control Message Event and pass it to current state and update new state
        val e = Event(controlMessage)
        fsmIsBusy.lock()
        try {
            feedEvent(e)
        } finally {
            fsmIsBusy.unlock()
        }
    }

    override fun onRatProverMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray) {
        processRatProverEvent(Event(controlMessage, Idscp2MessageHelper.createIdscpRatProverMessage(ratMessage)))
    }

    override fun onRatProverMessage(controlMessage: InternalControlMessage) {
        processRatProverEvent(Event(controlMessage))
    }

    /**
     * API for RatProver to provide Prover Messages to the fsm
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     *
     * Afterwards the fsm lock is requested
     *
     * When the RatProverThread does not match the active prover tread id, the event will be
     * ignored, else the event is provided to the fsm
     */
    private fun processRatProverEvent(e: Event) {
        //check for incorrect usage
        checkForFsmCycles()

        fsmIsBusy.lock()
        try {
            if (Thread.currentThread().id.toString() == currentRatProverId) {
                feedEvent(e)
            } else {
                LOG.error("An old or unknown Thread (${Thread.currentThread().id}) calls onRatProverMessage()")
            }
        } finally {
            fsmIsBusy.unlock()
        }
    }

    override fun onRatVerifierMessage(controlMessage: InternalControlMessage, ratMessage: ByteArray) {
        processRatVerifierEvent(Event(controlMessage, Idscp2MessageHelper.createIdscpRatVerifierMessage(ratMessage)))
    }

    override fun onRatVerifierMessage(controlMessage: InternalControlMessage) {
        processRatVerifierEvent(Event(controlMessage))
    }

    /**
     * API for RatVerifier to provide Verifier Messages to the fsm
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     *
     * Afterwards the fsm lock is requested
     *
     * When the RatVerifierDriver does not match the active verifier thread id, the event will be
     * ignored, else the event is provided to the fsm
     */
    private fun processRatVerifierEvent(e: Event) {
        //check for incorrect usage
        checkForFsmCycles()

        fsmIsBusy.lock()
        try {
            if (Thread.currentThread().id.toString() == currentRatVerifierId) {
                feedEvent(e)
            } else {
                LOG.error("An old or unknown Thread (${Thread.currentThread().id}) calls onRatVerifierMessage()")
            }
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /**
     * Feed the event to the current state and execute the runEntry method if the state has changed
     */
    private fun feedEvent(event: Event) {
        val prevState = currentState
        currentState = currentState!!.feedEvent(event)
        if (prevState != currentState) {
            currentState!!.runEntryCode(this)
        }
    }

    /**
     * API to terminate the idscp connection by the user
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     */
    fun closeConnection() {
        //check for incorrect usage
        checkForFsmCycles()
        LOG.debug("Sending stop message to connection peer...")
        onControlMessage(InternalControlMessage.IDSCP_STOP)
    }

    /**
     * API for the user to start the IDSCP2 handshake
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     */
    @Throws(Idscp2Exception::class)
    fun startIdscpHandshake() {
        //check for incorrect usage
        checkForFsmCycles()
        fsmIsBusy.lock()
        try {
            if (currentState == states[FsmState.STATE_CLOSED]) {
                if (fsmTerminated) {
                    throw Idscp2Exception("FSM is in a final closed state forever")
                }

                // trigger handshake init
                onControlMessage(InternalControlMessage.START_IDSCP_HANDSHAKE)

                // wait until handshake was successful or failed
                while (!handshakeResultAvailable) {
                    idscpHandshakeLock.await()
                }
                if (!isConnected && !fsmTerminated) {
                    //handshake failed, throw exception
                    throw Idscp2Exception("Handshake failed")
                }
            } else {
                throw Idscp2Exception("Handshake has already been started")
            }
        } catch (e: InterruptedException) {
            throw Idscp2Exception("Handshake failed because thread was interrupted")
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /**
     * Send idscp data from the fsm via the secure channel to the peer
     */
    fun sendFromFSM(msg: IdscpMessage?): Boolean {
        //send messages from fsm
        return secureChannel.send(msg!!.toByteArray())
    }

    /**
     * Provide an Internal Control Message to the FSM
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     */
    override fun onError(t: Throwable) {
        // Broadcast the error to the respective listeners
        connection.onError(t)

        // Check for incorrect usage
        checkForFsmCycles()
        onControlMessage(InternalControlMessage.ERROR)
    }

    /**
     * Provide an Internal Control Message to the FSM, caused by a secure channel closure
     *
     * The checkForFsmCycles method first checks for risky thread cycles that occur by incorrect
     * driver implementations
     */
    override fun onClose() {
        // Check for incorrect usage
        checkForFsmCycles()
        onControlMessage(InternalControlMessage.IDSCP_STOP)
    }

    /**
     * Send idscp message from the User via the secure channel
     */
    fun send(msg: ByteArray?) {
        // Send messages from user only when idscp connection is established
        idscpHandshakeCompletedLatch.await()
        fsmIsBusy.lock()
        try {
            if (isConnected) {
                val idscpMessage = Idscp2MessageHelper.createIdscpDataMessage(msg)
                if (!secureChannel.send(idscpMessage.toByteArray())) {
                    LOG.error("Cannot send IDSCP_DATA via secure channel")
                    onControlMessage(InternalControlMessage.ERROR)
                }
            } else {
                LOG.error("Cannot send IDSCP_DATA because connection is not established")
            }
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /**
     * Check if FSM is in STATE ESTABLISHED
     */
    val isConnected: Boolean
        get() = currentState == states[FsmState.STATE_ESTABLISHED]

    /**
     * Notify handshake lock about result
     */
    fun notifyHandshakeCompleteLock() {
        fsmIsBusy.lock()
        try {
            handshakeResultAvailable = true
            idscpHandshakeLock.signal()
            idscpHandshakeCompletedLatch.unlock()
        } finally {
            fsmIsBusy.unlock()
        }
    }

    /**
     * Calculate the RatProver mechanism
     *
     * @return The String of the cipher or null if no match was found
     */
    fun getRatProverMechanism(localSupportedProver: Array<String>, remoteExpectedVerifier: Array<String>): String {
        //toDo implement logic
        return localSupportedProver[0]
    }

    /**
     * Calculate the RatVerifier mechanism
     *
     * @return The String of the cipher or null if no match was found
     */
    fun getRatVerifierMechanism(localExpectedVerifier: Array<String>, remoteSupportedProver: Array<String>): String {
        //toDo implement logic
        return localExpectedVerifier[0]
    }

    /**
     * Stop current RatVerifier if active and start the RatVerifier from the
     * RatVerifierDriver Registry that matches the verifier mechanism
     *
     * @return false if no match was found
     */
    fun restartRatVerifierDriver(): Boolean {
        //assume verifier mechanism is set
        stopRatVerifierDriver()
        ratVerifierDriver = RatVerifierDriverRegistry.startRatVerifierDriver(verifierMechanism, this)
        return if (ratVerifierDriver == null) {
            LOG.error("Cannot create instance of RAT_VERIFIER_DRIVER")
            currentRatVerifierId = ""
            false
        } else {
            //safe the thread ID
            currentRatVerifierId = ratVerifierDriver!!.id.toString()
            LOG.debug("Start verifier_handshake timeout")
            verifierHandshakeTimer.resetTimeout(5)
            true
        }
    }

    /**
     * Terminate the RatVerifierDriver
     */
    fun stopRatVerifierDriver() {
        verifierHandshakeTimer.cancelTimeout()
        ratVerifierDriver?.let {
            if (it.isAlive) {
                it.interrupt()
                it.terminate()
            }
        }
    }

    /**
     * Stop current RatProver if active and start the RatProver from the
     * RatProverDriver Registry that matches the prover mechanism
     *
     * @return false if no match was found
     */
    fun restartRatProverDriver(): Boolean {
        //assume prover mechanism is set
        stopRatProverDriver()
        ratProverDriver = RatProverDriverRegistry.startRatProverDriver(proverMechanism, this)
        return if (ratProverDriver == null) {
            LOG.error("Cannot create instance of RAT_PROVER_DRIVER")
            currentRatProverId = ""
            false
        } else {
            //safe the thread ID
            currentRatProverId = ratProverDriver!!.id.toString()
            LOG.debug("Start prover_handshake timeout")
            proverHandshakeTimer.resetTimeout(5)
            true
        }
    }

    /**
     * Terminate the RatProverDriver
     */
    fun stopRatProverDriver() {
        proverHandshakeTimer.cancelTimeout()
        if (ratProverDriver != null && ratProverDriver!!.isAlive) {
            ratProverDriver!!.interrupt()
            ratProverDriver!!.terminate()
        }
    }

    /**
     * Lock the fsm forever, terminate the timers and drivers, close the secure channel
     * and notify handshake lock if necessary
     */
    fun shutdownFsm() {
        LOG.debug("Shutting down FSM of connection {}...", connection.id)
        if (LOG.isTraceEnabled) {
            LOG.trace("Running close handlers of connection {}...", connection.id)
        }
        connection.onClose()
        if (LOG.isTraceEnabled) {
            LOG.trace("Closing secure channel of connection {}...", connection.id)
        }
        secureChannel.close()
        if (LOG.isTraceEnabled) {
            LOG.trace("Clearing timeouts...")
        }
        datTimer.cancelTimeout()
        ratTimer.cancelTimeout()
        handshakeTimer.cancelTimeout()
        if (LOG.isTraceEnabled) {
            LOG.trace("Stopping RAT components...")
        }
        // Cancels proverHandshakeTimer
        stopRatProverDriver()
        // Cancels verifierHandshakeTimer
        stopRatVerifierDriver()
        if (LOG.isTraceEnabled) {
            LOG.trace("Mark FSM as terminated...")
        }
        fsmTerminated = true

        // Notify upper layer via handshake or closeListener
        if (!handshakeResultAvailable) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Notify handshake lock...")
            }
            notifyHandshakeCompleteLock()
        }
    }

    /**
     * Provide IDSCP2 message to the message listener
     */
    fun notifyIdscpMsgListener(data: ByteArray) {
        connection.onMessage(data)
        if (LOG.isTraceEnabled) {
            LOG.trace("Idscp data has been passed to connection listener")
        }
    }

    fun getState(state: FsmState?): State? {
        return states[state]
    }

    val isNotClosed: Boolean
        get() = currentState == getState(FsmState.STATE_CLOSED)

    fun setRatMechanisms(proverMechanism: String?, verifierMechanism: String?) {
        this.proverMechanism = proverMechanism
        this.verifierMechanism = verifierMechanism
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FSM::class.java)
    }

    init {
        val handshakeTimeoutHandler = Runnable {
            LOG.debug("HANDSHAKE_TIMER_EXPIRED")
            onControlMessage(InternalControlMessage.TIMEOUT)
        }
        val datTimeoutHandler = Runnable {
            LOG.debug("DAT_TIMER_EXPIRED")
            onControlMessage(InternalControlMessage.DAT_TIMER_EXPIRED)
        }
        val ratTimeoutHandler = Runnable {
            LOG.debug("RAT_TIMER_EXPIRED")
            onControlMessage(InternalControlMessage.REPEAT_RAT)
        }
        val proverTimeoutHandler = Runnable {
            LOG.debug("RAT_PROVER_HANDSHAKE_TIMER_EXPIRED")
            onControlMessage(InternalControlMessage.REPEAT_RAT)
        }
        val verifierTimeoutHandler = Runnable {
            LOG.debug("RAT_VERIFIER_HANDSHAKE_TIMER_EXPIRED")
            onControlMessage(InternalControlMessage.REPEAT_RAT)
        }
        handshakeTimer = Timer(fsmIsBusy, handshakeTimeoutHandler)
        datTimer = Timer(fsmIsBusy, datTimeoutHandler)
        ratTimer = Timer(fsmIsBusy, ratTimeoutHandler)
        proverHandshakeTimer = Timer(fsmIsBusy, proverTimeoutHandler)
        verifierHandshakeTimer = Timer(fsmIsBusy, verifierTimeoutHandler)

        /* ------------- FSM STATE Initialization -------------*/
        states[FsmState.STATE_CLOSED] = StateClosed(
                this, dapsDriver, onMessageBlock, localSupportedRatSuite, localExpectedRatSuite)
        states[FsmState.STATE_WAIT_FOR_HELLO] = StateWaitForHello(
                this, handshakeTimer, datTimer, dapsDriver, localSupportedRatSuite, localExpectedRatSuite)
        states[FsmState.STATE_WAIT_FOR_RAT] = StateWaitForRat(
                this, handshakeTimer, verifierHandshakeTimer, proverHandshakeTimer, ratTimer, ratTimeout, dapsDriver)
        states[FsmState.STATE_WAIT_FOR_RAT_PROVER] = StateWaitForRatProver(
                this, ratTimer, handshakeTimer, proverHandshakeTimer, dapsDriver)
        states[FsmState.STATE_WAIT_FOR_RAT_VERIFIER] = StateWaitForRatVerifier(
                this, dapsDriver, ratTimer, handshakeTimer, verifierHandshakeTimer, ratTimeout)
        states[FsmState.STATE_WAIT_FOR_DAT_AND_RAT] = StateWaitForDatAndRat(
                this, handshakeTimer, proverHandshakeTimer, datTimer, dapsDriver)
        states[FsmState.STATE_WAIT_FOR_DAT_AND_RAT_VERIFIER] = StateWaitForDatAndRatVerifier(
                this, handshakeTimer, datTimer, dapsDriver)
        states[FsmState.STATE_ESTABLISHED] = StateEstablished(
                this, dapsDriver, ratTimer, handshakeTimer)

        // Set initial state
        currentState = states[FsmState.STATE_CLOSED]
        this.connection = connection
        this.secureChannel = secureChannel
    }
}