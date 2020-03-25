package de.fhg.aisec.ids.idscp2.drivers.interfaces;

/**
 * An interface for the DAPS driver, which is used to verify and request dynamicAttributeTokens
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public interface DapsDriver {
    byte[] getToken();
    long verifyToken(byte[] dat, Object securityRequirements);
}
