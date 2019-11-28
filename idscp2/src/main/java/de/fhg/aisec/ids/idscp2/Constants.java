package de.fhg.aisec.ids.idscp2;

/**
 * Global IDSCPv2 protocol constants
 *
 * @author Leon Beckmann(leon.beckmann@aisec.fraunhofer.de)
 */

public final class Constants {

    //IDSCPv2 message types
    public static final int ATTESTATION_REQUEST_MSG = 0;
    public static final int ATTESTATION_RESPONSE_MSG = 1;
    public static final int METADATA_REQUEST_MSG = 2;
    public static final int METADATA_RESPONSE_MSG = 3;
    public static final int PAYLOAD_MSG = 4;


    //Standard Messages
    public static final String CLIENT_GOODBYE = "Client is disconnecting";
    public static final String SERVER_GOODBYE = "Server is disconnecting";
    public static final String END_OF_STREAM = "End of stream";

    /* TLS specific settings */

    //socket instances
    public static final String TLS_INSTANCE = "TLSv1.2";

    //enabled protocols
    public static final String[] TLS_ENABLED_PROTOCOLS = {TLS_INSTANCE};
    public static final String[] TLS_ENABLED_CIPHER = {
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
            /*"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"*/
    };


    private Constants(){};
}
