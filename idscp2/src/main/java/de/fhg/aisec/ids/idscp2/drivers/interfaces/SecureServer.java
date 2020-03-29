package de.fhg.aisec.ids.idscp2.drivers.interfaces;

/**
 * An interface for the IDSCPv2 Secure Server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureServer {

    /*
     * Terminate the secure server
     */
    void safeStop();

    /*
     * Check if the secure server is running
     */
    boolean isRunning();
}
