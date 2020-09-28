package de.fhg.aisec.ids.idscp2.idscp_core.fsm

/**
 * An enum that wraps the internal control messages of the IDSCP2 protocol to trigger transitions
 * by non-IDSCP2-message-events
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
enum class InternalControlMessage(
        val value: String) {
    // Using unique values that are different from IdscpMessage.MessageCase to identify event.key
    START_IDSCP_HANDSHAKE("ICM_START"),
    IDSCP_STOP("ICM_STOP"),
    DAT_TIMER_EXPIRED("ICM_DAT_TIMER_EXPIRED"),
    RAT_TIMER_EXPIRED("ICM_RAT_TIMER_EXPIRED"),
    REPEAT_RAT("ICM_REPEAT_RAT"),
    RAT_VERIFIER_OK("ICM_RAT_V_OK"),
    RAT_VERIFIER_FAILED("ICM_RAT_V_FAILED"),
    RAT_PROVER_OK("ICM_RAT_P_OK"),
    RAT_PROVER_FAILED("ICM_RAT_P_FAILED"),
    RAT_PROVER_MSG("ICM_RAT_PROVER_MSG"),
    RAT_VERIFIER_MSG("ICM_RAT_VERIFIER_MSG"),
    ERROR("ICM_ERROR"),
    TIMEOUT("ICM_TIMEOUT");
}