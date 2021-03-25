package de.fhg.aisec.ids.informationmodelmanager

import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.infomodel.ConnectorProfile
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.api.settings.Settings
import de.fraunhofer.iais.eis.*
import de.fraunhofer.iais.eis.ids.jsonld.Serializer
import de.fraunhofer.iais.eis.util.ConstraintViolationException
import de.fraunhofer.iais.eis.util.TypedLiteral
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ReferenceCardinality
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException
import java.util.*


/**
 * IDS Info Model Manager.
 */
@Component(name = "ids-infomodel-manager", immediate = true)
class InfoModelService : InfoModel {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private lateinit var settings: Settings
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var connectionManager: ConnectionManager? = null

    private val connectorProfile: ConnectorProfile
        get() = settings.connectorProfile

    override val modelVersion = BuildConfig.INFOMODEL_VERSION

    /**
     * Build Connector Entity Names from preferences
     */
    private val connectorEntityNames: List<TypedLiteral>
        get() = connectorProfile.connectorEntityNames.also {
            if (it == null) {
                LOG.warn("Settings or ConnectorProfile not available, or no connector entity names provided")
            }
        } ?: emptyList()

    private val catalog: ArrayList<ResourceCatalog>
        get() = arrayListOf(ResourceCatalogBuilder()._offeredResource_(ArrayList(resources)).build())

    /**
     * Build current endpoints as given by connectionManager
     * will not be stored in preferences, but freshly loaded each time.
     * Multiple names for one connector allowed
     */
    private val resources: List<Resource>
        get() = connectionManager?.let { cm ->
            cm.listAvailableEndpoints().map {
                val url = URI.create("${it.defaultProtocol}://${it.host}:${it.port}")
                val endpoint = ConnectorEndpointBuilder()._accessURL_(url).build()
                ResourceBuilder()._resourceEndpoint_(arrayListOf(endpoint)).build()
            }
        } ?: emptyList()

    /**
     * Build Security Profile from preferences
     * defaults to "NONE" for all attributes in case nothing has been stored
     * @return
     */
    private val securityProfile: SecurityProfile?
        get() = connectorProfile.securityProfile.also {
            if (it == null) {
                LOG.warn("Settings, ConnectorProfile not available, or no SecurityProfile provided")
            }
        }

    /**
     * Creates and returns Connector object based on stored preferences.
     * Returns random connector_url if none is stored in preferences.
     * The fields op_url and entityNames cannot be null.
     */
    override val connector: Connector?
        get() {
            val maintainerUrl = connectorProfile.maintainerUrl
            val connectorUrl = connectorProfile.connectorUrl
            val entityNames = connectorEntityNames

            if (LOG.isTraceEnabled) {
                LOG.trace("Maintainer URL: {}, Connector URL: {}, Entity Names: {}",
                        maintainerUrl, connectorUrl, entityNames)
            }

            return if (maintainerUrl != null) {
                try {
                    val trustedConnectorBuilder = if (connectorUrl == null) {
                        TrustedConnectorBuilder()
                    } else {
                        TrustedConnectorBuilder(connectorUrl)
                    }
                    trustedConnectorBuilder._maintainer_(maintainerUrl)
                            ._title_(ArrayList(entityNames))
                            ._securityProfile_(securityProfile)
                            ._resourceCatalog_(catalog)
                            ._description_(ArrayList(entityNames)).build()
                } catch (ex: ConstraintViolationException) {
                    LOG.error("Caught ConstraintViolationException while building Connector", ex)
                    null
                } catch (ex: URISyntaxException) {
                    LOG.error("Caught URISyntaxException while building Connector", ex)
                    null
                }
            } else {
                LOG.warn("Connector couldn't be built: Maintainer URL is required!")
                null
            }
        }

    /**
     * Store or update new Connector description to preferences.
     * Creates random connector_url if null and succeeds only if maintainerUrl and entityNames != null
     * Generates RDF description from Connector object and returns building success
     */
    override fun setConnector(profile: ConnectorProfile): Boolean {
        if (profile.securityProfile == null) {
            profile.securityProfile = SecurityProfile.TRUST_SECURITY_PROFILE
        }
        return run {
            settings.connectorProfile = profile

            try {
                connector != null
            } catch (ex: ConstraintViolationException) {
                LOG.error("ConstraintViolationException while building Connector.", ex)
                false
            }
        }
    }

    override val connectorAsJsonLd: String
        get() = settings.connectorJsonLd
                ?: connector?.let { serializer.serialize(it) }
                ?: throw NullPointerException("Connector is not available")

    override fun setConnectorByJsonLd(jsonLd: String?) {
        settings.let { settings ->
            if (jsonLd != null) {
                try {
                    serializer.deserialize(jsonLd, TrustedConnector::class.java)
                } catch (ex: Exception) {
                    LOG.error("Exception while parsing connector self-information.", ex)
                    throw ex
                }
            }
            settings.connectorJsonLd = jsonLd
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InfoModelService::class.java)
        private val serializer: Serializer by lazy { Serializer() }
    }

}
