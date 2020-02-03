package de.fhg.aisec.ids.idscp2.idscp_core.finite_state_machine;

import de.fhg.aisec.ids.messages.IDSCPv2;

public interface FsmListener {
    void onMessage(byte[] data);
    void onControlMessage(InternalControlMessage controlMessage);
    void onRatProverMessage(InternalControlMessage controlMessage, IDSCPv2.IdscpMessage idscpMessage);
    void onRatVerifierMessage(InternalControlMessage controlMessage, IDSCPv2.IdscpMessage idscpMessage);
    void onError();
    void onClose();
}
