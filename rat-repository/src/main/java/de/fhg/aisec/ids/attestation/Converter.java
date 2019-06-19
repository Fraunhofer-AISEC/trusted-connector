package de.fhg.aisec.ids.attestation;

import javax.xml.bind.DatatypeConverter;

public final class Converter {

  private Converter() {}

  public static String bytesToHex(byte[] bytes) {
    return DatatypeConverter.printHexBinary(bytes);
  }

  public static byte[] hexToBytes(String hex) {
    return DatatypeConverter.parseHexBinary(hex);
  }
}
