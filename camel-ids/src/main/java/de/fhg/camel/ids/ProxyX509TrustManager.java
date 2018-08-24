package de.fhg.camel.ids;

import de.fhg.ids.comm.CertificatePair;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SSLContextServerParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ProxyX509TrustManager implements X509TrustManager {

  public static void bindCertificatePair(SSLContextParameters sslContextParameters,
                                         boolean isServer, CertificatePair certificatePair)
      throws GeneralSecurityException, IOException {
    //TODO: Also retrieve own certificates from keystore
    if (isServer) {
      // Configure servers to ask for Client Certificates
      SSLContextServerParameters serverParameters = sslContextParameters.getServerParameters();
      if (serverParameters == null) {
        serverParameters = new SSLContextServerParameters();
      }
      serverParameters.setClientAuthentication("WANT");
      sslContextParameters.setServerParameters(serverParameters);
    } else {
      // CLIENT ONLY! For server side, get certificate from request
      // Replace X509TrustManager with proxy implementation to log certificates of communication partners
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
