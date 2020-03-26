package de.fhg.aisec.ids.api.tokenm;

public class DatException extends Exception {
  public DatException(String message) {
    super(message);
  }

  public DatException(String message, Throwable cause) {
    super(message, cause);
  }
}
