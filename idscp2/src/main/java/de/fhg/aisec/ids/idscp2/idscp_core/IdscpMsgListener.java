package de.fhg.aisec.ids.idscp2.idscp_core;

import de.fhg.aisec.ids.messages.IDSCPv2.IdscpMessage;

public interface IdscpMsgListener {
    void onMessage(IdscpMessage message);
}
