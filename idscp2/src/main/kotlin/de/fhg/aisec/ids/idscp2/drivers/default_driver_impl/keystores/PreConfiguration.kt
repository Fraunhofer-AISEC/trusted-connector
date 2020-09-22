package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * A class for creating pre-configured TrustManagers and KeyManagers for TLS Server and TLS Client
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
object PreConfiguration {
    private val LOG = LoggerFactory.getLogger(PreConfiguration::class.java)
    @Throws(KeyStoreException::class, IOException::class, CertificateException::class, NoSuchAlgorithmException::class)
    fun loadKeyStore(keyStorePath: Path, keyStorePassword: CharArray): KeyStore {
        val ks: KeyStore
        val pathString = keyStorePath.toString()
        ks = when {
            pathString.endsWith(".jks") -> {
                KeyStore.getInstance("JKS")
            }
            pathString.endsWith(".p12") -> {
                KeyStore.getInstance("PKCS12")
            }
            else -> {
                throw KeyStoreException("Unknown file extension \"" +
                        pathString.substring(pathString.lastIndexOf('.')) +
                        "\", only JKS (.jks) and PKCS12 (.p12) are supported.")
            }
        }
        Files.newInputStream(keyStorePath).use { keyStoreInputStream ->
            if (LOG.isDebugEnabled) {
                LOG.debug("Loading key store: $pathString")
            }
            ks.load(keyStoreInputStream, keyStorePassword)
        }
        return ks
    }

    /*
     * Get a secure X509ExtendedTrustManager for the SslContext
     *
     * throws IllegalStateException if number of available X509TrustManager is not one
     * throws RuntimeException of creating TrustManager fails
     */
    fun getX509ExtTrustManager(
            trustStorePath: Path,
            trustStorePassword: CharArray
    ): Array<TrustManager> {
        return try {
            /* create TrustManager */
            val myTrustManager: Array<TrustManager>
            val trustStore = loadKeyStore(trustStorePath, trustStorePassword)
            val trustManagerFactory = TrustManagerFactory.getInstance("PKIX") //PKIX from SunJSSE
            trustManagerFactory.init(trustStore)
            myTrustManager = trustManagerFactory.trustManagers

            /* set up TrustManager config */
            //allow only X509 Authentication
            if (myTrustManager.size == 1 && myTrustManager[0] is X509ExtendedTrustManager) {
                //toDo algorithm constraints
                myTrustManager
            } else {
                throw IllegalStateException("Unexpected default trust managers:" + myTrustManager.contentToString())
            }
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    /*
     * Get a secure X509ExtendedKeyManager for the SslContext
     *
     * throws IllegalStateException if number of available X509KeyManager is not one
     * throws RuntimeException of creating KeyManager fails
     */
    fun getX509ExtKeyManager(
            keyPassword: CharArray,
            keyStorePath: Path,
            keyStorePassword: CharArray,
            certAlias: String,
            keyType: String
    ): Array<KeyManager> {
        return try {
            /* create KeyManager for remote authentication */
            val myKeyManager: Array<KeyManager>
            val keystore = loadKeyStore(keyStorePath, keyStorePassword)
            val keyManagerFactory = KeyManagerFactory.getInstance("PKIX") //PKIX from SunJSSE
            keyManagerFactory.init(keystore, keyPassword)
            myKeyManager = keyManagerFactory.keyManagers

            /* set up keyManager config */
            //allow only X509 Authentication
            if (myKeyManager.size == 1 && myKeyManager[0] is X509ExtendedKeyManager) {
                //select certificate alias
                myKeyManager[0] = CustomX509ExtendedKeyManager(certAlias, keyType, myKeyManager[0] as X509ExtendedKeyManager)
                myKeyManager
            } else {
                throw IllegalStateException(
                        "Unexpected default key managers:" + myKeyManager.contentToString())
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        }
    }

    /*
     * Get a private key from a JKS Keystore
     *
     * throws RuntimeException if key is not available or key access was not permitted
     */
    fun getKey(
            keyStorePath: Path,
            keyStorePassword: CharArray,
            keyAlias: String,
            keyPassword: CharArray
    ): Key {
        return try {
            val keyStore = loadKeyStore(keyStorePath, keyStorePassword)

            // get private key
            val key = keyStore.getKey(keyAlias, keyPassword)
            key ?: throw RuntimeException("No key was found in keystore for given alias")
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException(e)
        }
    }

    /*
     * Get the certificate from the key store
     *
     * throws RuntimeException if key is not available or key access was not permitted
     */
    fun getCertificate(
            keyStorePath: Path,
            keyStorePassword: CharArray,
            keyAlias: String
    ): X509Certificate {
        return try {
            val keystore = loadKeyStore(keyStorePath, keyStorePassword)
            // get private key
            val cert = keystore.getCertificate(keyAlias) as X509Certificate
            // Probe key alias
            keystore.getKey(keyAlias, keyStorePassword)
            cert
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: KeyStoreException) {
            throw RuntimeException(e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: CertificateException) {
            throw RuntimeException(e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException(e)
        }
    } /*
     * This method can be used for filtering certificates in the trust store
     * to avoid expired certificates
     */
    //    private static PKIXBuilderParameters filterTrustAnchors(KeyStore keyStore, Date validityUntilDate)
    //            throws KeyStoreException, InvalidAlgorithmParameterException {
    //        PKIXParameters params = new PKIXParameters(keyStore);
    //
    //        // Obtain CA root certificates
    //        Set<TrustAnchor> myTrustAnchors = params.getTrustAnchors();
    //
    //        // Create new set of CA certificates that are still valid for specified date
    //        Set<TrustAnchor> validTrustAnchors =
    //                myTrustAnchors.stream().filter(
    //                        ta -> {
    //                            try {
    //                                ta.getTrustedCert().checkValidity(validityUntilDate);
    //                            } catch (CertificateException e) {
    //                                return false;
    //                            }
    //                            return true;
    //                        }).collect(Collectors.toSet());
    //
    //        // Create PKIXBuilderParameters parameters
    //        return new PKIXBuilderParameters(validTrustAnchors, new X509CertSelector());
    //    }
}