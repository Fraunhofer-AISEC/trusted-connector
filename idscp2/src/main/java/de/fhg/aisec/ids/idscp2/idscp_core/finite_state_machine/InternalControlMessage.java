package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

public enum InternalControlMessage {
    START_IDSCP_HANDSHAKE("ICM_START"),
    IDSCP_STOP("ICM_STOP"),
    DAT_TIMER_EXPIRED("ICM_DAT_TIMER_EXPIRED"),
    REPEAT_RAT("ICM_REPEAT_RAT"),
    RAT_VERIFIER_SUCCESSFUL("ICM_RAT_V_SUC"),
    RAT_VERIFIER_FAILED("ICM_RAT_V_FAILED"),
    RAT_PROVER_SUCCESSFUL("ICM_RAT_P_SUC"),
    RAT_PROVER_FAILED("ICM_RAT_P_FAILED"),
    DAT_VERIFICATION_SUCCESSFUL("IFM_DAT_SUC"),
    DAT_VERIFICATION_FAILED("ICM_DAT_FAILED"),
    ERROR("ICM_ERROR"),
    TIMEOUT("ICM_TIMEOUT"),
    SEND_IDSCP_MESSAGE("ICM_SEND");

    //set unique values that are different from IdscpMessage.MessageCase ot identify event.key
    private final String id;
    InternalControlMessage(String id) {this.id = id;}
    public String getValue() {return id;}
}
