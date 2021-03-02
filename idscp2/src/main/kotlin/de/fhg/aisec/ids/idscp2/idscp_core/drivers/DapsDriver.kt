package de.fhg.aisec.ids.idscp2.idscp_core.drivers

/**
 * An interface for the DAPS driver, which is used to verify and request dynamicAttributeTokens
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
interface DapsDriver {
    /*
     * Receive a token from the DapsDriver
     */
    val token: ByteArray

    /*
     * Verify a Daps token
     *
     * Return the number of seconds, the DAT is valid
     */
    fun verifyToken(dat: ByteArray): Long
}