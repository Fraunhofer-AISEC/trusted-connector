package de.fhg.aisec.ids.idscp2.default_drivers.daps

import java.nio.file.Path
import java.nio.file.Paths

/**
 * A Configuration class for the DefaultDapsDriver
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class DefaultDapsDriverConfig {
    var dapsUrl = "https://daps.aisec.fraunhofer.de"
        private set
    var keyStorePath: Path = Paths.get("DUMMY-FILENAME.p12")
        private set
    var keyStorePassword: CharArray = "password".toCharArray()
        private set
    var keyAlias = "1"
        private set
    var keyPassword: CharArray = "password".toCharArray()
        private set
    var trustStorePath: Path = Paths.get("DUMMY-FILENAME.p12")
        private set
    var trustStorePassword: CharArray = "password".toCharArray()
        private set

    class Builder {
        private val config = DefaultDapsDriverConfig()
        fun setDapsUrl(dapsUrl: String): Builder {
            config.dapsUrl = dapsUrl
            return this
        }

        fun setKeyStorePath(path: Path): Builder {
            config.keyStorePath = path
            return this
        }

        fun setKeyStorePassword(password: CharArray): Builder {
            config.keyStorePassword = password
            return this
        }

        fun setKeyAlias(alias: String): Builder {
            config.keyAlias = alias
            return this
        }

        fun setKeyPassword(password: CharArray): Builder {
            config.keyPassword = password
            return this
        }

        fun setTrustStorePath(path: Path): Builder {
            config.trustStorePath = path
            return this
        }

        fun setTrustStorePassword(password: CharArray): Builder {
            config.trustStorePassword = password
            return this
        }

        fun build(): DefaultDapsDriverConfig {
            return config
        }
    }
}