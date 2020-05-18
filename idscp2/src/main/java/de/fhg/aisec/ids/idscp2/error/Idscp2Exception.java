package de.fhg.aisec.ids.idscp2.error;

/**
 * IDSCP2 Exception
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class Idscp2Exception extends Exception{
    public Idscp2Exception(String message) {
        super(message);
    }

    public Idscp2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
