package de.fhg.aisec.ids.idscp2.default_drivers.daps

/**
 * A Security-Requirements class using Builder pattern to store the connectors expected
 * security attributes e.g. Audit Logging
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class SecurityRequirements {
    var requiredSecurityLevel: String? = null
        private set

    class Builder {
        private val requirements = SecurityRequirements()
        fun setRequiredSecurityLevel(requiredSecurityLevel: String?): Builder {
            requirements.requiredSecurityLevel = requiredSecurityLevel
            return this
        }

        fun build(): SecurityRequirements {
            return requirements
        }
    }
}