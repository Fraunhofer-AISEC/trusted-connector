package de.fhg.aisec.ids.idscp2;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class for creating pre-configured TrustManagers and KeyManagers for TLS Server and TLS Client
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class TLSPreConfiguration {

    public static TrustManager[] getX509ExtTrustManager(
            String trustStorePath,
            String trustStorePassword
    ) {
        try (
                InputStream jksTrustStoreIn = Files.newInputStream(Paths.get(trustStorePath))
        ) {
            /* create TrustManager */
            TrustManager[] myTrustManager;
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(jksTrustStoreIn, trustStorePassword.toCharArray());
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance("PKIX"); //PKIX from SunJSSE

            /* filterTrustAnchors for validation
            ManagerFactoryParameters trustParams =
                    new CertPathTrustManagerParameters(filterTrustAnchors(trustStore));
            trustManagerFactory.init(trustParams);*/
            trustManagerFactory.init(trustStore);

            myTrustManager = trustManagerFactory.getTrustManagers();


            /* set up TrustManager config */
            //allow only X509 Authentication
            if (myTrustManager.length == 1 && myTrustManager[0] instanceof X509ExtendedTrustManager) {
                //toDo hostname verification and algorithm constraints

                return myTrustManager;
            } else {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(myTrustManager));
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static KeyManager[] getX509ExtKeyManager(
            String keyStorePath,
            String keyStorePassword
    ) {
        try (
                InputStream jksKeyStoreIn = Files.newInputStream(Paths.get(keyStorePath));
        ) {
            /* create KeyManager for remote authentication */
            KeyManager[] myKeyManager;
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(jksKeyStoreIn, keyStorePassword.toCharArray());
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance("PKIX"); //PKIX from SunJSSE
            keyManagerFactory.init(keystore, keyStorePassword.toCharArray());
            myKeyManager = keyManagerFactory.getKeyManagers();

            /* set up keyManager config */
            //allow only X509 Authentication
            if (myKeyManager.length == 1 && myKeyManager[0] instanceof X509ExtendedKeyManager) {
                //toDo connection specific key selection via KeyAlias
                return myKeyManager;
            } else {
                throw new IllegalStateException(
                        "Unexpected default key managers:" + Arrays.toString(myKeyManager));
            }

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException
                | CertificateException e) {
            e.printStackTrace();
        }
        return null; //some exception was raised
    }

    private static PKIXBuilderParameters filterTrustAnchors(KeyStore keyStore)
            throws KeyStoreException, InvalidAlgorithmParameterException {
        PKIXParameters params = new PKIXParameters(keyStore);

        // Obtain CA root certificates
        Set<TrustAnchor> myTrustAnchors = params.getTrustAnchors();

        // Create new set of CA certificates that are still valid for specified date
        Set<TrustAnchor> validTrustAnchors =
                myTrustAnchors.stream().filter(
                        ta -> {
                            try {
                                ta.getTrustedCert().checkValidity(/* toDo expiration date*/);
                            } catch (CertificateException e) {
                                return false;
                            }
                            return true; }).collect(Collectors.toSet());

        // Create PKIXBuilderParameters parameters
        return new PKIXBuilderParameters(validTrustAnchors, new X509CertSelector());
    }
}