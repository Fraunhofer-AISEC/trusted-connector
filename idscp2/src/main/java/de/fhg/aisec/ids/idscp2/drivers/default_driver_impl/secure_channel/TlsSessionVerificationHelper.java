package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for verifying an established TLS Session on application layer
 *
 * @author Leon Beckmann (leon.beckmannn@aisec.fraunhofer.de)
 */
public class TlsSessionVerificationHelper {
  private static final Logger LOG = LoggerFactory.getLogger(TlsSessionVerificationHelper.class);

  /*
   * Returns true if the ssl session is valid and the remote host can be trusted
   *
   * Due to the fact, that hostname verification is not specified in the secure socket layer,
   * we have to check if the connected hostname matches to the subject of the peer certificate to
   * avoid Man-In-The-Middle Attacks.
   *
   * Further we check the peer certificate validity to avoid the case that some of the certificates
   * in our local trust_store are not valid anymore and allow a peer connector to connect with an
   * expired certificate
   */
  public static boolean verifyTlsSession(SSLSession sslSession) {

    LOG.debug("Connected to {}:{}", sslSession.getPeerHost(), sslSession.getPeerPort());
    try {
      Certificate[] certificates = sslSession.getPeerCertificates();

      if (certificates.length != 1) {
        LOG.warn("Unexpected number of certificates");
        return false;
      }
      X509Certificate peerCert = (X509Certificate) certificates[0];

      //check hostname verification
      Collection<List<?>> sans = peerCert.getSubjectAlternativeNames();
      if (sans == null) {
        LOG.warn("No Subject alternative names for hostname verification provided");
        return false;
      }

      ArrayList<String> hostNames = new ArrayList<>();

      for (List<?> subjectAltName : sans) {
        if (subjectAltName.size() != 2) {
          continue;
        }
        Object value = subjectAltName.get(1);
        switch ((Integer)subjectAltName.get(0)) {
          case 2: //DNS_NAME
          case 7: //IP_ADDRESS
            if (value instanceof String) {
              hostNames.add((String)value);
            } else if (value instanceof byte[]) {
              hostNames.add(new String((byte[]) value));
            }
            break;
          case 0: //OTHER_NAME - Not Supported
          case 1: //RFC_822_Name - Not Supported
          case 3: //X400_ADDRESS - Not Supported
          case 4: //DIRECTORY_NAME - Not Supported
          case 5: //EDI_PARTY_NAME - Not supported
          case 6: //URI - Not Supported
          case 8: //REGISTERED_ID - Not Supported
          default: //unspecified General Name - should never happen
            break;
        }
      }

      //toDo localhost is matched manually to 127.0.0.1 for testing.. a matching file ip <-> dns
      // would be nicer in the future
      if (hostNames.contains("localhost")) {
        hostNames.add("127.0.0.1");
      }

      if (!hostNames.contains(sslSession.getPeerHost())) {
        LOG.warn("Hostname verification failed. Peer certificate does not belong to peer host");
        return false;
      }

      //check certificate validity for now and at least one day
      Date oneDay = new Date();
      oneDay.setTime(oneDay.getTime() + 86400000);

      peerCert.checkValidity();
      peerCert.checkValidity(oneDay);

    } catch (SSLPeerUnverifiedException | CertificateParsingException |
        CertificateNotYetValidException | CertificateExpiredException e) {
      LOG.warn("TLS Session Verification failed", e);
      return false;
    }

    return true;
  }

}
