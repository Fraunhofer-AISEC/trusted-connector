package de.fhg.aisec.ids.idscp2.drivers.interfaces

/**
 * An interface for the IDSCP2 Secure Server
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface SecureServer {
    /*
     * Terminate the secure server
     */
    fun safeStop()

    /*
     * Check if the secure server is running
     */
    val isRunning: Boolean
}