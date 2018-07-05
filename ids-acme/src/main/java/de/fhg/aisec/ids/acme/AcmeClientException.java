package de.fhg.aisec.ids.acme;

public class AcmeClientException extends RuntimeException {
    public AcmeClientException(String message) {
        super(message);
    }

    public AcmeClientException(Throwable cause) {
        super(cause);
    }
}
