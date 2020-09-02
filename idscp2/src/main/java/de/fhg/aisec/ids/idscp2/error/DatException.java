package de.fhg.aisec.ids.idscp2.error;

public class DatException extends RuntimeException {
    public DatException(String s) {
        super(s);
    }

    public DatException(String s, Exception e) {
        super(s, e);
    }
}
