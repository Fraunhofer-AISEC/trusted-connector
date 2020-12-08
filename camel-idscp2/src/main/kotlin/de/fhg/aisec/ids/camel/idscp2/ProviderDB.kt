package de.fhg.aisec.ids.camel.idscp2

import de.fraunhofer.iais.eis.ContractAgreement
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object ProviderDB {
    val availableArtifactURIs: ConcurrentHashMap<URI, String> = ConcurrentHashMap()
    val artifactUrisMapped2ContractAgreements: ConcurrentHashMap<URI, URI> = ConcurrentHashMap()
    val contractAgreements: ConcurrentHashMap<URI, ContractAgreement> = ConcurrentHashMap()

    init {
        availableArtifactURIs[URI.create("https://example.com/some_artifact")] = "AVAILABLE"
    }
}