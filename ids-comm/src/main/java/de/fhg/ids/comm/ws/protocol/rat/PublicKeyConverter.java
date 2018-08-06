/*-
 * ========================LICENSE_START=================================
 * ids-comm
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.ids.comm.ws.protocol.rat;

import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class PublicKeyConverter {
  private static final String openSslFixedHeader = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA";
  private byte[] midHeader = new byte[2];
  private byte[] exponent = new byte[3];
  private PublicKey key = null;
  private KeyFactory kf;
  private X509EncodedKeySpec spec;
  private byte[] keyBuffer;
  private byte[] modulus;

  public PublicKeyConverter(TPM2B_PUBLIC publicKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    keyBuffer =
        this.setExponent(this.setMidHeader(this.setModulus(this.setFixedHeader(), publicKey)));
    spec = new X509EncodedKeySpec(keyBuffer);
    kf = KeyFactory.getInstance("RSA");
    this.key = kf.generatePublic(spec);
  }

  private byte[] setModulus(byte[] key, TPM2B_PUBLIC publicKey) {
    this.modulus =
        PublicKeyConverter.combineByteArray(key, publicKey.getPublicArea().getUnique().getBuffer());
    return this.modulus;
  }

  public PublicKey getPublicKey() {
    return this.key;
  }

  private byte[] setFixedHeader() {
    return Base64.getDecoder().decode(openSslFixedHeader);
  }

  private byte[] setMidHeader(byte[] key) {
    // Mid-header is always 0x02 0x03 i.e. the exponent is a 3 bytes (0x03) integer (0x02)
    midHeader[0] = 0x02;
    midHeader[1] = 0x03;
    return PublicKeyConverter.combineByteArray(key, midHeader);
  }

  private byte[] setExponent(byte[] key) {
    // Exponent is always 65537 (2^16+1)
    exponent[0] = 0x01;
    exponent[1] = 0x00;
    exponent[2] = 0x01;
    return PublicKeyConverter.combineByteArray(key, exponent);
  }

  private static byte[] combineByteArray(byte[] one, byte[] two) {
    byte[] three = new byte[one.length + two.length];
    System.arraycopy(one, 0, three, 0, one.length);
    System.arraycopy(two, 0, three, one.length, two.length);
    return three;
  }

  public byte[] getExponent() {
    return this.exponent;
  }

  public byte[] getModulus() {
    return this.modulus;
  }

  public byte[] getDER() {
    return this.keyBuffer;
  }
}
