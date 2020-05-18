package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;

public class TPM2dHelper {
  private static final Logger LOG = LoggerFactory.getLogger(TPM2dHelper.class);
  private static final SecureRandom sr = new SecureRandom();

  private TPM2dHelper() {}

  /**
   * Generate a crypto-secure random hex String of length numChars
   *
   * @param numBytes Desired String length
   * @return The generated crypto-secure random hex String
   */
  static byte[] generateNonce(int numBytes) {
    byte[] randBytes = new byte[numBytes];
    sr.nextBytes(randBytes);
    return randBytes;
  }

  /**
   * Calculate SHA-1 hash of (nonce|certificate).
   *
   * @param nonce The plain, initial nonce
   * @param certificate The certificate to hash-combine with the nonce
   * @return The new nonce, updated with the given certificate using SHA-1
   */
  static byte[] calculateHash(byte[] nonce, @Nullable Certificate certificate) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(nonce);
      if (certificate != null) {
        digest.update(certificate.getEncoded());
      } else {
        if (LOG.isWarnEnabled()) {
          LOG.warn(
              "No client certificate available. Cannot bind nonce to public key to prevent masquerading attack. TLS misconfiguration!");
        }
      }
      return digest.digest();
    } catch (Exception e1) {
      LOG.error("Could not create hash of own nonce and local certificate", e1);
      return nonce;
    }
  }

  static class ByteArrayUtil {

    private static final String[] lookup = new String[256];

    static {
      for (int i = 0; i < lookup.length; ++i) {
        if (i < 16) {
          lookup[i] = "0" + Integer.toHexString(i);
        } else {
          lookup[i] = Integer.toHexString(i);
        }
      }
    }

    static String toPrintableHexString(byte[] bytes) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < bytes.length; ++i) {
        if (i > 0 && i % 16 == 0) {
          s.append('\n');
        } else {
          s.append(' ');
        }
        s.append(lookup[bytes[i] & 0xff]);
      }
      return s.toString();
    }
  }

}
