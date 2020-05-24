package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for verifying an established TLS Session on application layer
 * (application level security)
 *
 * @author Leon Beckmann (leon.beckmannn@aisec.fraunhofer.de)
 */
public class TLSSessionVerificationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(TLSSessionVerificationHelper.class);

    private static final String ipv4Pattern =
            "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    /*
     * Checks if the ssl session is valid and the remote host can be trusted
     *
     * Due to the fact, that hostname verification is not specified in the secure socket layer,
     * we have to check if the connected hostname matches to the subject of the peer certificate to
     * avoid Man-In-The-Middle Attacks. This is required for every raw tls!
     *
     * Further we check the peer certificate validity to avoid the case that some of the certificates
     * in our local trust_store are not valid anymore and allow a peer connector to connect with an
     * expired certificate
     *
     * Throws SSlPeerUnverifiedException if peer certificate is not secure for this peer
     */
    public static void verifyTlsSession(SSLSession sslSession) throws SSLPeerUnverifiedException {

        String host = sslSession.getPeerHost();

        LOG.debug("Connected to {}:{}", host, sslSession.getPeerPort());
        try {

            //get certificate
            Certificate[] certificates = sslSession.getPeerCertificates();
            if (certificates.length != 1) {
                throw new SSLPeerUnverifiedException("Unexpected number of certificates");
            }
            X509Certificate peerCert = (X509Certificate) certificates[0];

            /*
             * According to RFC6125, hostname verification should be done against the certificate's
             * subject alternative name's (SANs) dNSName field or the SANs IPAddress. In some legacy
             * implementations, the check is done against the certificate's commonName, but this is
             * deprecated for quite a while and is therefore not supported anymore in te IDSCP2 protocol.
             */
            Collection<List<?>> sans = peerCert.getSubjectAlternativeNames();
            if (sans == null) {
                throw new SSLPeerUnverifiedException("No Subject alternative names for hostname "
                        + "verification provided");
            }

            ArrayList<String> acceptedDnsNames = new ArrayList<>();
            ArrayList<String> acceptedIpAddresses = new ArrayList<>();

            for (List<?> subjectAltName : sans) {
                if (subjectAltName.size() != 2) {
                    continue;
                }
                Object value = subjectAltName.get(1);
                switch ((Integer) subjectAltName.get(0)) {
                    case 2: //DNS_NAME
                        if (value instanceof String) {
                            acceptedDnsNames.add((String) value);
                        } else if (value instanceof byte[]) {
                            acceptedDnsNames.add(new String((byte[]) value));
                        }
                        break;
                    case 7: //IP_ADDRESS
                        if (value instanceof String) {
                            acceptedIpAddresses.add((String) value);
                        } else if (value instanceof byte[]) {
                            acceptedIpAddresses.add(new String((byte[]) value));
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

            //toDo localhost is matched manually to 127.0.0.1 for testing..
            // automatic dns resolving via DNS service is not an option since we cannot trust the DNS
            if (acceptedDnsNames.contains("localhost")) {
                acceptedIpAddresses.add("127.0.0.1");
            }


            if (isIpAddress(host)) {
                //FIXME The server should provide a possibility to validate clients dnsNames against SANs in
                // Client certificate to avoid MITMs. This is an open issue
                //check ip addresses RFC 2818 (Section 3.1)
                if (!acceptedIpAddresses.contains(host)) {
                    throw new SSLPeerUnverifiedException("Hostname verification failed. Peer certificate does "
                            + "not belong to peer host");
                }
            } else {
                //check hostname
                String[] hostLabels = host.split("\\.");
                boolean found = false;

                for (String entry : acceptedDnsNames) {
                    if (checkHostname(entry.split("\\.", -1), hostLabels)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new SSLPeerUnverifiedException("Hostname verification failed. Peer certificate does "
                            + "not belong to peer host");
                }
            }


            //check certificate validity for now and at least one day
            Date oneDay = new Date();
            oneDay.setTime(oneDay.getTime() + 86400000);

            peerCert.checkValidity();
            peerCert.checkValidity(oneDay);

        } catch (CertificateParsingException | CertificateNotYetValidException |
                CertificateExpiredException e) {
            throw new SSLPeerUnverifiedException("TLS Session Verification failed " + e);
        }
    }


    /*
     * check if host is an IP Address
     */
    private static boolean isIpAddress(String host) {

        Matcher ip4 = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE).matcher(host);
        if (ip4.matches()) {
            return true;
        }

        Matcher ip6 = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE).matcher(host);
        return ip6.matches();
    }

    /*
     * match dNS Name
     */
    private static boolean checkHostname(String[] dnsNameLabels, String[] hostNameLabels) {

        /*
         * support wildcard matching of DNS names as described in RFC6125 Section 6.4.3
         *
         * Rules:
         * 1. The client SHOULD NOT attempt to match a presented identifier in which the wildcard
         * character comprises a label other than the left-most label
         * (e.g., do not match bar.*.example.net).
         *
         * 2. If the wildcard character is the only character of the left-most label in the
         * presented identifier, the client SHOULD NOT compare against anything but the left-most
         * label of the reference identifier (e.g., *.example.com would match foo.example.com but
         * not bar.foo.example.com or example.com).
         *
         * 3. The client MAY match a presented identifier in which the wildcard character is not the
         * only character of the label (e.g., baz*.example.net and *baz.example.net and
         * b*z.example.net would be taken to match baz1.example.net and foobaz.example.net and
         * buzz.example.net, respectively).  However, the client SHOULD NOT attempt to match a
         * presented identifier where the wildcard character is embedded within an A-label or
         * U-label of an internationalized domain name.
         */
        if (dnsNameLabels.length == hostNameLabels.length) { //include rule 2
            //all labels without the first one must match completely (rule 1)
            for (int i = 1; i < dnsNameLabels.length; i++) {
                if (!dnsNameLabels[i].equals(hostNameLabels[i])) {
                    return false;
                }
            }

            //first label could include wildcard character '*' (rule 1+3)
            return hostNameLabels[0].matches(dnsNameLabels[0].replace("*", ".*"));
        }

        return false;
    }

}
