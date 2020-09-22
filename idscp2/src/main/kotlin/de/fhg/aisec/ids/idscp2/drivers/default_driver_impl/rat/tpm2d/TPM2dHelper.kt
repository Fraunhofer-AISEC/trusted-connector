package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.rat.tpm2d

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.Certificate

object TPM2dHelper {
    private val LOG = LoggerFactory.getLogger(TPM2dHelper::class.java)
    private val sr = SecureRandom()

    /**
     * Generate a crypto-secure random hex String of length numChars
     *
     * @param numBytes Desired String length
     * @return The generated crypto-secure random hex String
     */
    fun generateNonce(numBytes: Int): ByteArray {
        val randBytes = ByteArray(numBytes)
        sr.nextBytes(randBytes)
        return randBytes
    }

    /**
     * Calculate SHA-1 hash of (nonce|certificate).
     *
     * @param nonce       The plain, initial nonce
     * @param certificate The certificate to hash-combine with the nonce
     * @return The new nonce, updated with the given certificate using SHA-1
     */
    fun calculateHash(nonce: ByteArray, certificate: Certificate?): ByteArray {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            digest.update(nonce)
            if (certificate != null) {
                digest.update(certificate.encoded)
            } else {
                if (LOG.isWarnEnabled) {
                    LOG.warn(
                            "No client certificate available. Cannot bind nonce to public key to prevent masquerading attack. TLS misconfiguration!")
                }
            }
            digest.digest()
        } catch (e1: Exception) {
            LOG.error("Could not create hash of own nonce and local certificate", e1)
            nonce
        }
    }

    internal object ByteArrayUtil {
        private val lookup = arrayOfNulls<String>(256)
        fun toPrintableHexString(bytes: ByteArray): String {
            val s = StringBuilder()
            for (i in bytes.indices) {
                if (i > 0 && i % 16 == 0) {
                    s.append('\n')
                } else {
                    s.append(' ')
                }
                s.append(lookup[bytes[i].toInt() and 0xff])
            }
            return s.toString()
        }

        init {
            for (i in lookup.indices) {
                if (i < 16) {
                    lookup[i] = "0" + Integer.toHexString(i)
                } else {
                    lookup[i] = Integer.toHexString(i)
                }
            }
        }
    }
}