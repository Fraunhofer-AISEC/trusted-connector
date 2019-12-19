package de.fhg.aisec.ids.idscp2.drivers.interfaces;

/**
 *
 *
 * Developer API
 *
 * Methods:
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface SecureServer {
    void safeStop();
    boolean isRunning();
}
