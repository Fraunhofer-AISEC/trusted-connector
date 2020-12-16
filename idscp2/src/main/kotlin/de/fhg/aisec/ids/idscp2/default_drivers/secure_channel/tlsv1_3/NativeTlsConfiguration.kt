package de.fhg.aisec.ids.idscp2.default_drivers.secure_channel.tlsv1_3

import java.nio.file.Path
import java.nio.file.Paths

/**
 * NativeTLS SecureChannel configuration class, contains information about NativeTLS stuff
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class NativeTlsConfiguration {
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
    var keyStoreKeyType = "RSA"
        private set

    class Builder {
        private val config = NativeTlsConfiguration()
        fun setHost(host: String): Builder {
            config.host = host
            return this
        }

        fun setServerPort(serverPort: Int): Builder {
            config.serverPort = serverPort
            return this
        }

        fun setKeyPassword(pwd: CharArray): Builder {
            config.keyPassword = pwd
            return this
        }

        fun setTrustStorePath(path: Path): Builder {
            config.trustStorePath = path
            return this
        }

        fun setKeyStorePath(path: Path): Builder {
            config.keyStorePath = path
            return this
        }

        fun setTrustStorePassword(pwd: CharArray): Builder {
            config.trustStorePassword = pwd
            return this
        }

        fun setKeyStorePassword(pwd: CharArray): Builder {
            config.keyStorePassword = pwd
            return this
        }

        fun setCertificateAlias(alias: String): Builder {
            config.certificateAlias = alias
            return this
        }

        fun setKeyStoreKeyType(keyType: String): Builder {
            config.keyStoreKeyType = keyType
            return this
        }

        fun build(): NativeTlsConfiguration {
            return config
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NativeTlsConfiguration

        if (serverPort != other.serverPort) return false
        if (host != other.host) return false
        if (trustStorePath != other.trustStorePath) return false
        if (!trustStorePassword.contentEquals(other.trustStorePassword)) return false
        if (!keyPassword.contentEquals(other.keyPassword)) return false
        if (keyStorePath != other.keyStorePath) return false
        if (!keyStorePassword.contentEquals(other.keyStorePassword)) return false
        if (certificateAlias != other.certificateAlias) return false
        if (keyStoreKeyType != other.keyStoreKeyType) return false

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
        result = 31 * result + keyStoreKeyType.hashCode()
        return result
    }

    override fun toString(): String {
        return "Idscp2Configuration(serverPort=$serverPort, host='$host', trustStorePath=$trustStorePath, " +
                "trustStorePassword=${trustStorePassword.contentToString()}, " +
                "keyStorePath=$keyStorePath, keyStorePassword=${keyStorePassword.contentToString()}, " +
                "certificateAlias='$certificateAlias', " +
                "keyStoreKeyType='$keyStoreKeyType'"
    }

    companion object {
        const val DEFAULT_SERVER_PORT = 29292
    }
}