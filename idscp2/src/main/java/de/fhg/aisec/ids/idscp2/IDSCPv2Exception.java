package de.fhg.aisec.ids.idscp2;

public class IDSCPv2Exception extends RuntimeException{
    public IDSCPv2Exception(String message) {
        super(message);
    }

    public IDSCPv2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
