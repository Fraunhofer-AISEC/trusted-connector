package de.fhg.aisec.ids.idscp2.drivers.interfaces;

import de.fhg.aisec.ids.messages.IDSCPv2;

public interface RatProverInstance {
    void delegate(IDSCPv2.IdscpMessage message);
    void terminate();
}
