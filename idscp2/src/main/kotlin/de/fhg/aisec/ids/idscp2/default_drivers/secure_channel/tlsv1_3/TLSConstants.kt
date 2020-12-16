package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.tlsv1_3

/**
 * TLS Constants
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
object TLSConstants {
    // Used TLS version
    const val TLS_INSTANCE = "TLSv1.3"

    // Enabled encryption protocols
    val TLS_ENABLED_PROTOCOLS = arrayOf(TLS_INSTANCE)

    // Acceptable TLS ciphers
    val TLS_ENABLED_CIPHERS = arrayOf( //            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",  //            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",  //            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            //            "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
            //            "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384")
}