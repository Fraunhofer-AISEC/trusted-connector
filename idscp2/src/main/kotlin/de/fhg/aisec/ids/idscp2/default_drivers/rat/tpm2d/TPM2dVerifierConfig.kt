package de.fhg.aisec.ids.idscp2.default_drivers.rat.tpm2d

import de.fhg.aisec.ids.idscp2.messages.Tpm2dAttestation
import java.net.URI
import java.net.URISyntaxException
import java.security.cert.Certificate

/**
 * A configuration class for TPM2d RatVerifier Driver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class TPM2dVerifierConfig private constructor() {
    var localCertificate: Certificate? = null
        private set
    var ttpUri: URI? = null
        private set
    var expectedAType = Tpm2dAttestation.IdsAttestationType.BASIC
        private set
    var expectedAttestationMask = 0
        private set

    class Builder {
        fun setTtpUri(ttpUri: URI): Builder {
            config.ttpUri = ttpUri
            return this
        }

        fun setLocalCertificate(localCert: Certificate): Builder {
            config.localCertificate = localCert
            return this
        }

        fun setExpectedAttestationType(aType: Tpm2dAttestation.IdsAttestationType): Builder {
            config.expectedAType = aType
            return this
        }

        fun setExpectedAttestationMask(mask: Int): Builder {
            config.expectedAttestationMask = mask
            return this
        }

        fun build(): TPM2dVerifierConfig {
            return config
        }

        companion object {
            private val config = TPM2dVerifierConfig()
        }
    }

    init {
        ttpUri = try {
            URI("https://invalid-ttp-uri/rat-verify")
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
}