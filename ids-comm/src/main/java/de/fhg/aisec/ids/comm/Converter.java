package de.fhg.aisec.ids.comm;

import com.google.protobuf.ByteString;

import javax.xml.bind.DatatypeConverter;

public final class Converter {

  private Converter() {}

  public static String bytesToHex(byte[] bytes) {
    return DatatypeConverter.printHexBinary(bytes);
  }

  public static String byteStringToHex(ByteString byteString) {
    return bytesToHex(byteString.toByteArray());
  }

  public static byte[] hexToBytes(String hex) {
    return DatatypeConverter.parseHexBinary(hex);
  }

  public static ByteString hexToByteString(String hex) {
    return ByteString.copyFrom(hexToBytes(hex));
  }
}
