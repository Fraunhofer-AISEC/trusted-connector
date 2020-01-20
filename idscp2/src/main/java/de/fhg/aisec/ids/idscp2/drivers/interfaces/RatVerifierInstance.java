package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.messages.IDSCPv2;

public interface RatVerifierInstance {
    void delegate(IDSCPv2.IdscpMessage message);
    void terminate();
    void restart();
}
