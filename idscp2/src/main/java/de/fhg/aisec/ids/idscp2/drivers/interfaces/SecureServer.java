package de.fhg.aisec.ids.idscp2.drivers.interfaces;

/**
 * An interface for the IDSCPv2 Secure Server
 *
 * Developer API
 *
 * Methods:
 * void safeStop()          to terminate the server
 * boolean isRunning()      to check if the server is running
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureServer {
    void safeStop();
    boolean isRunning();
}
