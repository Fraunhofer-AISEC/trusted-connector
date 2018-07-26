package de.fhg.ids.comm.ws.protocol;

public class IDSCPException extends RuntimeException {
    public IDSCPException(String message) {
        super(message);
    }

    public IDSCPException(String message, Throwable cause) {
        super(message, cause);
    }
}
