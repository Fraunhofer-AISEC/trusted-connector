package de.fhg.aisec.ids.idscp2.idscp_core.configuration

/**
 * Attestation configuration class, containing attestation suite for supported / expected
 * attestation types
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
class AttestationConfig {
    val ratMechanisms: Array<String>
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AttestationConfig
        return ratMechanisms.contentEquals(that.ratMechanisms)
    }

    override fun hashCode(): Int {
        return ratMechanisms.contentHashCode()
    }

    companion object {
        val DEFAULT_RAT_MECHANISMS = arrayOf("Dummy", "TPM2d")
    }

    init {
        ratMechanisms = DEFAULT_RAT_MECHANISMS
    }
}