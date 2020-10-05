package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.secure_channel

import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession

/**
 * A class for verifying an established TLS Session on application layer
 * (application level security)
 *
 * @author Leon Beckmann (leon.beckmannn@aisec.fraunhofer.de)
 */
object TLSSessionVerificationHelper {
    private val LOG = LoggerFactory.getLogger(TLSSessionVerificationHelper::class.java)
    private const val ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    private const val ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}"

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
    @Throws(SSLPeerUnverifiedException::class)
    fun verifyTlsSession(sslSession: SSLSession) {
        val host = sslSession.peerHost
        LOG.debug("Connected to {}:{}", host, sslSession.peerPort)
        try {

            //get certificate
            val certificates = sslSession.peerCertificates
            if (certificates.size != 1) {
                throw SSLPeerUnverifiedException("Unexpected number of certificates")
            }
            val peerCert = certificates[0] as X509Certificate

            /*
             * According to RFC6125, hostname verification should be done against the certificate's
             * subject alternative name's (SANs) DNSName field or the SANs IPAddress. In some legacy
             * implementations, the check is done against the certificate's commonName, but this is
             * deprecated for quite a while and is therefore not supported anymore in the IDSCP2 protocol.
             */
            val sans = peerCert.subjectAlternativeNames
                    ?: throw SSLPeerUnverifiedException("No Subject alternative names for hostname "
                            + "verification provided")
            val acceptedDnsNames = ArrayList<String>()
            val acceptedIpAddresses = ArrayList<String>()
            for (subjectAltName in sans) {
                if (subjectAltName.size != 2) {
                    continue
                }
                val value = subjectAltName[1]
                when (subjectAltName[0] as Int?) {
                    2 -> if (value is String) {
                        acceptedDnsNames.add(value)
                    } else if (value is ByteArray) {
                        acceptedDnsNames.add(String(value))
                    }
                    7 -> if (value is String) {
                        acceptedIpAddresses.add(value)
                    } else if (value is ByteArray) {
                        acceptedIpAddresses.add(String(value))
                    }
                    else -> {
                        if (LOG.isDebugEnabled) {
                            LOG.debug("Unhandled SAN type \"{}\" with value \"{}\"", subjectAltName[0], value)
                        }
                    }
                }
            }

            if (isIpAddress(host)) {
                // First, check IP addresses directly given by type-7-SANs
                if (!acceptedIpAddresses.contains(host)) {
                    // Check IP addresses using DNS resolving
                    // This check is *weak* and should be accompanied by DAT fingerprint checking later on
                    val resolvedIps = acceptedDnsNames.flatMap {
                        try {
                            InetAddress.getAllByName(it).toList()
                        } catch (e: Throwable) {
                            emptyList()
                        }
                    }.map { it.hostAddress }
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Resolved IPs: {}", resolvedIps.toSet().joinToString())
                    }
                    if (!resolvedIps.contains(host)) {
                        throw SSLPeerUnverifiedException("Hostname verification failed. Peer certificate does "
                                + "not belong to peer host")
                    }
                }
            } else {
                // Check hostname
                val hostLabels = host.split(".")
                var found = false
                for (entry in acceptedDnsNames) {
                    if (checkHostname(entry.trimEnd('.').split("."), hostLabels)) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    throw SSLPeerUnverifiedException("Hostname verification failed. Peer certificate does "
                            + "not belong to peer host")
                }
            }


            //check certificate validity for now and at least one day
            val oneDay = Date()
            oneDay.time = oneDay.time + 86400000
            peerCert.checkValidity()
            peerCert.checkValidity(oneDay)
        } catch (e: CertificateParsingException) {
            throw SSLPeerUnverifiedException("TLS Session Verification failed $e")
        } catch (e: CertificateNotYetValidException) {
            throw SSLPeerUnverifiedException("TLS Session Verification failed $e")
        } catch (e: CertificateExpiredException) {
            throw SSLPeerUnverifiedException("TLS Session Verification failed $e")
        }
    }

    /*
     * check if host is an IP Address
     */
    private fun isIpAddress(host: String): Boolean {
        val ip4 = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE).matcher(host)
        if (ip4.matches()) {
            return true
        }
        val ip6 = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE).matcher(host)
        return ip6.matches()
    }

    /*
     * match dNS Name
     */
    private fun checkHostname(dnsNameLabels: List<String>, hostNameLabels: List<String>): Boolean {

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
        if (dnsNameLabels.size == hostNameLabels.size) { //include rule 2
            //all labels without the first one must match completely (rule 1)
            for (i in 1 until dnsNameLabels.size) {
                if (dnsNameLabels[i] != hostNameLabels[i]) {
                    return false
                }
            }

            //first label could include wildcard character '*' (rule 1+3)
            return hostNameLabels[0].matches(Regex(dnsNameLabels[0].replace("*", ".*")))
        }
        return false
    }
}