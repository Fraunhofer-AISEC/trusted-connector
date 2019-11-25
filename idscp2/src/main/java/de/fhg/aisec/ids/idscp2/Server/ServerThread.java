package de.fhg.aisec.ids.idscp2.Server;

/**
 * A server thread interface for the IDSCPv2 protocol.
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */

public interface ServerThread {
    void send(byte[] bytes);
    void safeStop();
    boolean isConnected();
}
