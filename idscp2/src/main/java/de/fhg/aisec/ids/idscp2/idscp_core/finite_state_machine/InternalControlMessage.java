package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

public enum InternalControlMessage {
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

    //set unique values that are different from IdscpMessage.MessageCase ot identify event.key
    private final String id;
    InternalControlMessage(String id) {this.id = id;}
    public String getValue() {return id;}
}
