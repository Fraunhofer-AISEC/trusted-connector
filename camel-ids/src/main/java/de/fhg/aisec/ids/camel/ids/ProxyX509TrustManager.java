package de.fhg.aisec.ids.camel.ids;

import de.fhg.aisec.ids.comm.CertificatePair;
import org.apache.camel.util.jsse.*;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class ProxyX509TrustManager implements X509TrustManager {

  public static void bindCertificatePair(SSLContextParameters sslContextParameters,
                                         boolean isServer, CertificatePair certificatePair)
      throws GeneralSecurityException, IOException {
    // Acquire the local certificate
    if (sslContextParameters.getKeyManagers() != null) {
      KeyManagersParameters keyManagers = sslContextParameters.getKeyManagers();
      if (keyManagers.getKeyStore() != null) {
        KeyStoreParameters keyStoreParameters = keyManagers.getKeyStore();
        KeyStore keyStore = keyStoreParameters.createKeyStore();
        // If a certificate alias is set, use it
        String alias = sslContextParameters.getCertAlias();

        // Otherwise, the first alias with a private key is relevant for us
        if (alias == null) {
          Enumeration<String> es = keyStore.aliases();
          while (es.hasMoreElements()) {
            alias = es.nextElement();
            if (keyStore.isKeyEntry(alias)) {
              break;
            }
          }
        }
        // Now assign the certificate from the key store to the pair
        if (alias != null) {
          certificatePair.setLocalCertificate(keyStore.getCertificate(alias));
        }
      }
    }
    // Acquire the remote server certificate OR just configure the server
    if (isServer) {
      // SERVER ONLY: Configure server to ask for client certificates
      SSLContextServerParameters serverParameters = sslContextParameters.getServerParameters();
      if (serverParameters == null) {
        serverParameters = new SSLContextServerParameters();
      }
      serverParameters.setClientAuthentication("WANT");
      sslContextParameters.setServerParameters(serverParameters);
    } else {
      // CLIENT ONLY: Replace X509TrustManager with this proxy implementation to log certificate of remote server
      TrustManagersParameters tmParams = sslContextParameters.getTrustManagers();
      X509TrustManager systemTrustManager = (X509TrustManager) tmParams.createTrustManagers()[0];
      tmParams.setTrustManager(new ProxyX509TrustManager(systemTrustManager, certificatePair));
    }
  }

  private final X509TrustManager trustManager;
  private final CertificatePair certificatePair;

  public ProxyX509TrustManager(X509TrustManager trustManager, CertificatePair certificatePair) {
    this.trustManager = trustManager;
    this.certificatePair = certificatePair;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    trustManager.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    // Save observed server certificate
    certificatePair.setRemoteCertificate(chain[0]);
    trustManager.checkServerTrusted(chain, authType);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return trustManager.getAcceptedIssuers();
  }
}
