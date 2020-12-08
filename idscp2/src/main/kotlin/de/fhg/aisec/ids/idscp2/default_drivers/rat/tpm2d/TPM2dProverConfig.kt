package de.fhg.aisec.ids.idscp2.default_drivers.rat.tpm2d

import java.security.cert.Certificate

/**
 * A configuration class for TPM2d RatPriver driver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TPM2dProverConfig private constructor() {
    var remoteCertificate: Certificate? = null
        private set
    var tpm2dHost: String
        private set

    class Builder {
        fun setRemoteCertificate(remoteCert: Certificate?): Builder {
            config.remoteCertificate = remoteCert
            return this
        }

        fun setTpmHost(host: String): Builder {
            config.tpm2dHost = host
            return this
        }

        fun build(): TPM2dProverConfig {
            return config
        }

        companion object {
            private val config = TPM2dProverConfig()
        }
    }

    init {
        tpm2dHost = if (System.getenv("TPM_HOST") != null) System.getenv("TPM_HOST") else "localhost"
    }
}