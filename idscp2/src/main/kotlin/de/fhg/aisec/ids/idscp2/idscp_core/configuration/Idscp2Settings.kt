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
class Idscp2Settings {
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
    var supportedAttestation = AttestationConfig()
        private set
    var expectedAttestation = AttestationConfig()
        private set
    var ratTimeoutDelay = DEFAULT_RAT_TIMEOUT_DELAY.toInt().toLong()
        private set

    class Builder {
        private val settings = Idscp2Settings()
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

        fun setSupportedAttestation(suite: AttestationConfig): Builder {
            settings.supportedAttestation = suite
            return this
        }

        fun setExpectedAttestation(suite: AttestationConfig): Builder {
            settings.expectedAttestation = suite
            return this
        }

        fun setRatTimeoutDelay(delay: Long): Builder {
            settings.ratTimeoutDelay = delay
            return this
        }

        fun build(): Idscp2Settings {
            return settings
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Idscp2Settings
        return serverPort == that.serverPort && ratTimeoutDelay == that.ratTimeoutDelay &&
                host == that.host &&
                trustStorePath == that.trustStorePath &&
                trustStorePassword.contentEquals(that.trustStorePassword) &&
                keyStorePath == that.keyStorePath &&
                keyStorePassword.contentEquals(that.keyStorePassword) &&
                certificateAlias == that.certificateAlias &&
                dapsKeyAlias == that.dapsKeyAlias &&
                keyStoreKeyType == that.keyStoreKeyType &&
                supportedAttestation == that.supportedAttestation &&
                expectedAttestation == that.expectedAttestation
    }

    override fun hashCode(): Int {
        return Objects.hash(serverPort, host, trustStorePath, trustStorePassword, keyStorePath,
                keyStorePassword, certificateAlias, dapsKeyAlias, keyStoreKeyType, supportedAttestation,
                expectedAttestation, ratTimeoutDelay)
    }

    override fun toString(): String {
        return "Idscp2Settings(serverPort=$serverPort, host='$host', trustStorePath=$trustStorePath, " +
                "trustStorePassword=${trustStorePassword.contentToString()}, " +
                "keyPassword=${keyPassword.contentToString()}, keyStorePath=$keyStorePath, " +
                "keyStorePassword=${keyStorePassword.contentToString()}, certificateAlias='$certificateAlias', " +
                "dapsKeyAlias='$dapsKeyAlias', keyStoreKeyType='$keyStoreKeyType', " +
                "supportedAttestation=$supportedAttestation, expectedAttestation=$expectedAttestation, " +
                "ratTimeoutDelay=$ratTimeoutDelay)"
    }


    companion object {
        const val DEFAULT_SERVER_PORT = 29292
        const val DEFAULT_RAT_TIMEOUT_DELAY = "600"
    }
}