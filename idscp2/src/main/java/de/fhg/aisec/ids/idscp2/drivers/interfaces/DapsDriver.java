package de.fhg.aisec.ids.idscp2.drivers.interfaces;

/**
 * An interface for the DAPS driver, which is used to verify and request dynamicAttributeTokens
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface DapsDriver {

    /*
     * Receive a token from the DapsDriver
     */
    byte[] getToken();

    /*
     * Verify a Daps token
     *
     * An optional security requirements object can be provided to validate the DAT body
     *
     * Return the number of seconds, the DAT is valid
     */
    long verifyToken(byte[] dat, Object securityRequirements);
}
