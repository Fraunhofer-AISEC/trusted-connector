package de.fhg.aisec.ids.idscp2.idscp_core.api.configuration

import java.util.*

/**
 * Attestation configuration class, containing attestation suite for supported / expected
 * attestation types
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class AttestationConfig {
    lateinit var supportedAttestationSuite: Array<String>
        private set
    lateinit var expectedAttestationSuite: Array<String>
        private set
    var ratTimeoutDelay = DEFAULT_RAT_TIMEOUT_DELAY.toInt().toLong() // in ms
        private set

    class Builder {
        private val config = AttestationConfig()

        fun setSupportedRatSuite(suite: Array<String>): Builder {
            config.supportedAttestationSuite = suite
            return this
        }

        fun setExpectedRatSuite(suite: Array<String>): Builder {
            config.expectedAttestationSuite = suite
            return this
        }

        fun setRatTimeoutDelay(delay: Long): Builder {
            config.ratTimeoutDelay = delay
            return this
        }

        fun build(): AttestationConfig {
            return config
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AttestationConfig
        return supportedAttestationSuite.contentEquals(that.supportedAttestationSuite) &&
                expectedAttestationSuite.contentEquals(that.expectedAttestationSuite) &&
                ratTimeoutDelay == that.ratTimeoutDelay
    }

    override fun hashCode(): Int {
        return Objects.hash(supportedAttestationSuite.contentHashCode(),
                expectedAttestationSuite.contentHashCode(), ratTimeoutDelay)
    }

    companion object {
        const val DEFAULT_RAT_TIMEOUT_DELAY = "3600000" // in ms: 1 hour
    }
}