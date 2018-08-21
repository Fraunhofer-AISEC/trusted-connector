package de.fhg.camel.ids;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SSLContextServerParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class ProxyX509TrustManager implements X509TrustManager {

  public static void patchSslContextParameters(SSLContextParameters sslContextParameters)
      throws GeneralSecurityException, IOException {
    // Configure servers to ask for Client Certificates
    SSLContextServerParameters serverParameters = sslContextParameters.getServerParameters();
    if (serverParameters == null) {
      serverParameters = new SSLContextServerParameters();
    }
    serverParameters.setClientAuthentication("WANT");
    sslContextParameters.setServerParameters(serverParameters);
    // Replace X509TrustManager with proxy implementation to log certificates of communication partners
    TrustManagersParameters tmParams = sslContextParameters.getTrustManagers();
    X509TrustManager systemTrustManager = (X509TrustManager) tmParams.createTrustManagers()[0];
    tmParams.setTrustManager(new ProxyX509TrustManager(systemTrustManager));
  }

  private final X509TrustManager tm;

  public ProxyX509TrustManager(X509TrustManager tm) {
    this.tm = tm;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    System.out.println("################################################################################");
    System.out.println("#################################### CLIENT ####################################");
    System.out.println("################################################################################");
    System.out.println(Arrays.toString(chain));
    tm.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    System.out.println("################################################################################");
    System.out.println("#################################### SERVER ####################################");
    System.out.println("################################################################################");
    System.out.println(Arrays.toString(chain));
    tm.checkServerTrusted(chain, authType);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return tm.getAcceptedIssuers();
  }
}
