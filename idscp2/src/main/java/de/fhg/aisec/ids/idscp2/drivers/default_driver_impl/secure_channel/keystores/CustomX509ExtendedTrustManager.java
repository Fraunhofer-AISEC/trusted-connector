package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel.keystores;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * currently not in use, we will use the https hostname verification instead
 *
 * A custom X509ExtendedTrustManager for hostname verification and algorithm constraints,
 * which is an application protocol task and avoids Man-In-The-Middle Attacks
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class CustomX509ExtendedTrustManager extends X509ExtendedTrustManager {
    /*
    * verifying that the certificate itself can be trusted (RFC 3280/5280) and verifying the identity in the certificate (RFC 6125, or RFC 2818 for HTTPS)
    * https://tersesystems.com/blog/2014/03/23/fixing-hostname-verification/
    * https://tools.ietf.org/html/rfc6125
    * https://tools.ietf.org/search/rfc6125
    *
    *
    * sslParams.setEndpointIdentificationAlgorithm("HTTPS"); //makes identity verification in the ssl context
    *
    *
    * If the socket parameter is an instance of SSLSocket, and the endpoint identification algorithm of the
    * SSLParameters is non-empty, to prevent man-in-the-middle attacks, the address that the socket connected to
    * should be checked against the peer's identity presented in the end-entity X509 certificate, as specified in
    * the endpoint identification algorithm.
    *
    *
    * If the socket parameter is an instance of SSLSocket, and the algorithm constraints of the SSLParameters is
    * non-null, for every certificate in the certification path, fields such as subject public key, the signature
    * algorithm, key usage, extended key usage, etc. need to conform to the algorithm constraints in place on this
    * socket.*/

    private X509ExtendedTrustManager delegate;

    public CustomX509ExtendedTrustManager(X509ExtendedTrustManager delegate){
        this.delegate = delegate;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        delegate.checkClientTrusted(x509Certificates, s, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        delegate.checkServerTrusted(x509Certificates, s, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        delegate.checkClientTrusted(x509Certificates, s, sslEngine);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        delegate.checkServerTrusted(x509Certificates, s, sslEngine);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        delegate.checkClientTrusted(x509Certificates, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        delegate.checkServerTrusted(x509Certificates, s);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
