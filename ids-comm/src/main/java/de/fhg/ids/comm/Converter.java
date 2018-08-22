package de.fhg.ids.comm;

import com.google.protobuf.ByteString;

import javax.xml.bind.DatatypeConverter;

public final class Converter {

  private Converter() {}

  public static String byteStringToHex(ByteString byteString) {
    return DatatypeConverter.printHexBinary(byteString.toByteArray());
  }

  public static ByteString hexToByteString(String hex) {
    return ByteString.copyFrom(DatatypeConverter.parseHexBinary(hex));
  }
}
