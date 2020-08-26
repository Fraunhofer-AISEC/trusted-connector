package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public static KeyStore getKeyStoreInstanceByPath(String path) throws KeyStoreException {
        if (path.endsWith(".jks")) {
            return KeyStore.getInstance("JKS");
        } else if (path.endsWith(".p12")) {
            return KeyStore.getInstance("PKCS12");
        } else {
            throw new KeyStoreException("Unknown file extension \"" + path.substring(path.lastIndexOf('.')) +
                    "\", " + "only JKS (.jks) and PKCS12 (.p12) are supported.");
        }
    }

    /*
     * Get a secure X509ExtendedTrustManager for the SslContext
     *
     * throws IllegalStateException if number of available X509TrustManager is not one
     * throws RuntimeException of creating TrustManager fails
     */
    public static TrustManager[] getX509ExtTrustManager(
            String trustStorePath,
            String trustStorePassword
    ) {
        try (
                InputStream trustStoreInputStream = Files.newInputStream(Paths.get(trustStorePath))
        ) {
            /* create TrustManager */
            final TrustManager[] myTrustManager;
            final KeyStore trustStore = getKeyStoreInstanceByPath(trustStorePath);
            trustStore.load(trustStoreInputStream, trustStorePassword.toCharArray());
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
            String keyPassword,
            String keyStorePath,
            String keyStorePassword,
            String certAlias,
            String keyType
    ) {
        try (
                InputStream keyStoreInputStream = Files.newInputStream(Paths.get(keyStorePath))
        ) {
            /* create KeyManager for remote authentication */
            final KeyManager[] myKeyManager;
            KeyStore keystore = getKeyStoreInstanceByPath(keyStorePath);
            keystore.load(keyStoreInputStream, keyStorePassword.toCharArray());
            final KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance("PKIX"); //PKIX from SunJSSE
            keyManagerFactory.init(keystore, keyPassword.toCharArray());
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
            String keyStorePath,
            String keyStorePassword,
            String keyAlias
    ) {
        try (
                InputStream keyStoreInputStream = Files.newInputStream(Paths.get(keyStorePath))
        ) {
            /* create KeyManager for remote authentication */
            KeyStore keystore = getKeyStoreInstanceByPath(keyStorePath);

            //load keystore
            keystore.load(keyStoreInputStream, keyStorePassword.toCharArray());

            // get private key
            Key key = keystore.getKey(keyAlias, keyStorePassword.toCharArray());
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
        String keyStorePath,
        String keyStorePassword,
        String keyAlias
    ) {
        try (
            InputStream keyStoreInputStream = Files.newInputStream(Paths.get(keyStorePath))
        ) {
            /* create KeyManager for remote authentication */
            KeyStore keystore = getKeyStoreInstanceByPath(keyStorePath);

            //load keystore
            keystore.load(keyStoreInputStream, keyStorePassword.toCharArray());

            // get private key
            X509Certificate cert = (X509Certificate) keystore.getCertificate(keyAlias);
            // Probe key alias
            keystore.getKey(keyAlias, keyStorePassword.toCharArray());
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