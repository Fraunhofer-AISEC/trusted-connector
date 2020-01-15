package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

public interface FsmListener {
    void onMessage(byte[] data);
    void onControlMessage(InternalControlMessage controlMessage);
}
