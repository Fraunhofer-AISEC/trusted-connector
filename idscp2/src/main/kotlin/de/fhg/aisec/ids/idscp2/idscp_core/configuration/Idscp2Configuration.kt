package de.fhg.aisec.ids.idscp2.idscp_core.configuration

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * IDSCP2 configuration class, contains information about keyStore and TrustStores,
 * Attestation Types, host, DAPS, ...
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class Idscp2Configuration {
    var serverPort = DEFAULT_SERVER_PORT
        private set
    var host = "localhost"
        private set
    var trustStorePath: Path = Paths.get("DUMMY-FILENAME.p12")
        private set
    var trustStorePassword = "password".toCharArray()
        private set
    var keyPassword = "password".toCharArray()
        private set
    var keyStorePath: Path = Paths.get("DUMMY-FILENAME.p12")
        private set
    var keyStorePassword = "password".toCharArray()
        private set
    var certificateAlias = "1.0.1"
        private set
    var dapsKeyAlias = "1"
        private set
    var keyStoreKeyType = "RSA"
        private set
    lateinit var attestationConfig: AttestationConfig
        private set
    var handshakeTimeoutDelay = DEFAULT_HANDSHAKE_TIMEOUT_DELAY.toInt().toLong()
        private set
    var ackTimeoutDelay = DEFAULT_ACK_TIMEOUT_DELAY.toInt().toLong()
        private set

    class Builder {
        private val settings = Idscp2Configuration()
        fun setHost(host: String): Builder {
            settings.host = host
            return this
        }

        fun setServerPort(serverPort: Int): Builder {
            settings.serverPort = serverPort
            return this
        }

        fun setKeyPassword(pwd: CharArray): Builder {
            settings.keyPassword = pwd
            return this
        }

        fun setTrustStorePath(path: Path): Builder {
            settings.trustStorePath = path
            return this
        }

        fun setKeyStorePath(path: Path): Builder {
            settings.keyStorePath = path
            return this
        }

        fun setTrustStorePassword(pwd: CharArray): Builder {
            settings.trustStorePassword = pwd
            return this
        }

        fun setKeyStorePassword(pwd: CharArray): Builder {
            settings.keyStorePassword = pwd
            return this
        }

        fun setCertificateAlias(alias: String): Builder {
            settings.certificateAlias = alias
            return this
        }

        fun setDapsKeyAlias(alias: String): Builder {
            settings.dapsKeyAlias = alias
            return this
        }

        fun setKeyStoreKeyType(keyType: String): Builder {
            settings.keyStoreKeyType = keyType
            return this
        }

        fun setAttestationConfig(config: AttestationConfig): Builder {
            settings.attestationConfig = config
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

        if (serverPort != other.serverPort) return false
        if (host != other.host) return false
        if (trustStorePath != other.trustStorePath) return false
        if (!trustStorePassword.contentEquals(other.trustStorePassword)) return false
        if (!keyPassword.contentEquals(other.keyPassword)) return false
        if (keyStorePath != other.keyStorePath) return false
        if (!keyStorePassword.contentEquals(other.keyStorePassword)) return false
        if (certificateAlias != other.certificateAlias) return false
        if (dapsKeyAlias != other.dapsKeyAlias) return false
        if (keyStoreKeyType != other.keyStoreKeyType) return false
        if (attestationConfig != other.attestationConfig) return false
        if (handshakeTimeoutDelay != other.handshakeTimeoutDelay) return false
        if (ackTimeoutDelay != other.ackTimeoutDelay) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverPort
        result = 31 * result + host.hashCode()
        result = 31 * result + trustStorePath.hashCode()
        result = 31 * result + trustStorePassword.contentHashCode()
        result = 31 * result + keyPassword.contentHashCode()
        result = 31 * result + keyStorePath.hashCode()
        result = 31 * result + keyStorePassword.contentHashCode()
        result = 31 * result + certificateAlias.hashCode()
        result = 31 * result + dapsKeyAlias.hashCode()
        result = 31 * result + keyStoreKeyType.hashCode()
        result = 31 * result + attestationConfig.hashCode()
        result = 31 * result + handshakeTimeoutDelay.hashCode()
        result = 31 * result + ackTimeoutDelay.hashCode()
        return result
    }

    override fun toString(): String {
        return "Idscp2Configuration(serverPort=$serverPort, host='$host', trustStorePath=$trustStorePath, " +
                "trustStorePassword=${trustStorePassword.contentToString()}, " +
                "keyStorePath=$keyStorePath, keyStorePassword=${keyStorePassword.contentToString()}, " +
                "certificateAlias='$certificateAlias', dapsKeyAlias='$dapsKeyAlias', " +
                "keyStoreKeyType='$keyStoreKeyType', attestationConfig=$attestationConfig, " +
                "handshakeTimeoutDelay=$handshakeTimeoutDelay, ackTimeoutDelay=$ackTimeoutDelay)"
    }

    companion object {
        const val DEFAULT_SERVER_PORT = 29292
        const val DEFAULT_ACK_TIMEOUT_DELAY = "1"
        const val DEFAULT_HANDSHAKE_TIMEOUT_DELAY = "5"
    }
}