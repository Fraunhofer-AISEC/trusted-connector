package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * A class for creating pre-configured TrustManagers and KeyManagers for TLS Server and TLS Client
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class PreConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PreConfiguration.class);

    public static KeyStore loadKeyStore(Path keyStorePath, char[] keyStorePassword)
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore ks;
        final var pathString = keyStorePath.toString();
        if (pathString.endsWith(".jks")) {
            ks = KeyStore.getInstance("JKS");
        } else if (pathString.endsWith(".p12")) {
            ks = KeyStore.getInstance("PKCS12");
        } else {
            throw new KeyStoreException("Unknown file extension \"" + pathString.substring(pathString.lastIndexOf('.')) +
                    "\", " + "only JKS (.jks) and PKCS12 (.p12) are supported.");
        }
        try (InputStream keyStoreInputStream = Files.newInputStream(keyStorePath)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading key store: " + pathString);
            }
            ks.load(keyStoreInputStream, keyStorePassword);
        }
        return ks;
    }

    /*
     * Get a secure X509ExtendedTrustManager for the SslContext
     *
     * throws IllegalStateException if number of available X509TrustManager is not one
     * throws RuntimeException of creating TrustManager fails
     */
    public static TrustManager[] getX509ExtTrustManager(
            Path trustStorePath,
            char[] trustStorePassword
    ) {
        try {
            /* create TrustManager */
            final TrustManager[] myTrustManager;
            final KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword);
            final TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance("PKIX"); //PKIX from SunJSSE

            trustManagerFactory.init(trustStore);
            myTrustManager = trustManagerFactory.getTrustManagers();

            /* set up TrustManager config */
            //allow only X509 Authentication
            if (myTrustManager.length == 1 && myTrustManager[0] instanceof X509ExtendedTrustManager) {
                //toDo algorithm constraints
                return myTrustManager;
            } else {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(myTrustManager));
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Get a secure X509ExtendedKeyManager for the SslContext
     *
     * throws IllegalStateException if number of available X509KeyManager is not one
     * throws RuntimeException of creating KeyManager fails
     */
    public static KeyManager[] getX509ExtKeyManager(
            char[] keyPassword,
            Path keyStorePath,
            char[] keyStorePassword,
            String certAlias,
            String keyType
    ) {
        try {
            /* create KeyManager for remote authentication */
            final KeyManager[] myKeyManager;
            KeyStore keystore = loadKeyStore(keyStorePath, keyStorePassword);
            final KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance("PKIX"); //PKIX from SunJSSE
            keyManagerFactory.init(keystore, keyPassword);
            myKeyManager = keyManagerFactory.getKeyManagers();

            /* set up keyManager config */
            //allow only X509 Authentication
            if (myKeyManager.length == 1 && myKeyManager[0] instanceof X509ExtendedKeyManager) {
                //select certificate alias
                myKeyManager[0] =
                        new CustomX509ExtendedKeyManager(certAlias, keyType, (X509ExtendedKeyManager) myKeyManager[0]);
                return myKeyManager;
            } else {
                throw new IllegalStateException(
                        "Unexpected default key managers:" + Arrays.toString(myKeyManager));
            }

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException
                | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Get a private key from a JKS Keystore
     *
     * throws RuntimeException if key is not available or key access was not permitted
     */
    public static Key getKey(
            Path keyStorePath,
            char[] keyStorePassword,
            String keyAlias,
            char[] keyPassword
    ) {
        try {
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword);

            // get private key
            Key key = keyStore.getKey(keyAlias, keyPassword);
            if (key == null) {
                throw new RuntimeException("No key was found in keystore for given alias");
            } else {
                return key;
            }

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
                | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Get the certificate from the key store
     *
     * throws RuntimeException if key is not available or key access was not permitted
     */
    public static X509Certificate getCertificate(
        Path keyStorePath,
        char[] keyStorePassword,
        String keyAlias
    ) {
        try {
            KeyStore keystore = loadKeyStore(keyStorePath, keyStorePassword);

            // get private key
            X509Certificate cert = (X509Certificate) keystore.getCertificate(keyAlias);
            // Probe key alias
            keystore.getKey(keyAlias, keyStorePassword);
            if (cert == null) {
                throw new RuntimeException("No cert was found in keystore for given alias");
            } else {
                return cert;
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
            | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /*
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