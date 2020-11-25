package de.fhg.aisec.ids.idscp2.idscp_core.api.configuration

import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import java.nio.file.Path
import java.nio.file.Paths

/**
 * IDSCP2 configuration class, contains information about Attestation Types, DAPS, Timeouts,
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2Configuration {
    lateinit var attestationConfig: AttestationConfig
        private set
    lateinit var dapsDriver: DapsDriver
        private set
    var handshakeTimeoutDelay = DEFAULT_HANDSHAKE_TIMEOUT_DELAY.toInt().toLong() //in ms
        private set
    var ackTimeoutDelay = DEFAULT_ACK_TIMEOUT_DELAY.toInt().toLong() //in ms
        private set

    class Builder {
        private val settings = Idscp2Configuration()

        fun setAttestationConfig(config: AttestationConfig): Builder {
            settings.attestationConfig = config
            return this
        }

        fun setDapsDriver(dapsDriver: DapsDriver): Builder {
            settings.dapsDriver = dapsDriver
            return this
        }

        fun setHandshakeTimeoutDelay(delay: Long): Builder {
            settings.handshakeTimeoutDelay = delay
            return this
        }

        fun setAckTimeoutDelay(delay: Long): Builder {
            settings.ackTimeoutDelay = delay
            return this
        }

        fun build(): Idscp2Configuration {
            return settings
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Idscp2Configuration

        if (attestationConfig != other.attestationConfig) return false
        if (dapsDriver != other.dapsDriver) return false
        if (handshakeTimeoutDelay != other.handshakeTimeoutDelay) return false
        if (ackTimeoutDelay != other.ackTimeoutDelay) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attestationConfig.hashCode()
        result = 31 *  result + dapsDriver.hashCode()
        result = 31 * result + handshakeTimeoutDelay.hashCode()
        result = 31 * result + ackTimeoutDelay.hashCode()
        return result
    }

    override fun toString(): String {
        return "Idscp2Configuration(attestationConfig=$attestationConfig, " +
                "dapsDriver=$dapsDriver, handshakeTimeoutDelay=$handshakeTimeoutDelay, " +
                "ackTimeoutDelay=$ackTimeoutDelay)"
    }

    companion object {
        const val DEFAULT_ACK_TIMEOUT_DELAY = "200" // (in ms)
        const val DEFAULT_HANDSHAKE_TIMEOUT_DELAY = "5000" // (in ms)
    }
}