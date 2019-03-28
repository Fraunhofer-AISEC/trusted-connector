package de.fhg.aisec.ids.informationmodelmanager

import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.infomodel.ConnectorProfile
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.api.settings.Settings
import de.fraunhofer.iais.eis.*
import de.fraunhofer.iais.eis.ids.jsonld.Serializer
import de.fraunhofer.iais.eis.util.ConstraintViolationException
import de.fraunhofer.iais.eis.util.PlainLiteral
import de.fraunhofer.iais.eis.util.Util
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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var settings: Settings? = null
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var connectionManager: ConnectionManager? = null

    /**
     * Build Connector Entity Names from preferences
     */
    private val connectorEntityNames: List<PlainLiteral>
        get() {
            val entityNames = settings?.connectorProfile?.connectorEntityNames
            return if (entityNames != null) {
                entityNames
            } else {
                LOG.warn("Settings or ConnectorProfile not available")
                emptyList()
            }
        }

    private val catalog: Catalog
        get() {
            return CatalogBuilder()._offers_(ArrayList(resources)).build()
        }

    /**
     * Build current endpoints as given by connectionManager
     * will not be stored in preferences, but freshly loaded each time.
     * Multiple names for one connector allowed
     */
    private val resources: List<Resource>
        get() {
            return connectionManager?.let { cm ->
                cm.listAvailableEndpoints().map {
                    val url = URI.create("${it.defaultProtocol}://${it.host}:${it.port}")
                    val host = HostBuilder()._accessUrl_(url)._protocol_(Protocol.IDSCP).build()
                    val endpoint = StaticEndpointBuilder()._endpointHost_(host).build()
                    ResourceBuilder()._resourceEndpoints_(arrayListOf(endpoint)).build()
                }
            } ?: emptyList()
        }

    /**
     * Build Security Profile from preferences
     * defaults to "NONE" for all attributes in case nothing has been stored
     * @return
     */
    private val securityProfile: SecurityProfile?
        get() {
            val securityProfile = settings?.connectorProfile?.securityProfile
            return if (securityProfile != null) {
                securityProfile
            } else {
                LOG.warn("Settings or ConnectorProfile not available")
                null
            }
        }

    // creates and returns Connector object based on stored preferences
    // returns random connector_url if none is stored in preferences
    // op_url and entityNames can not be null
    override fun getConnector(): Connector? {
        val maintainerUrl = settings?.connectorProfile?.maintainerUrl
        val connectorUrl = settings?.connectorProfile?.connectorUrl
        val entityNames = connectorEntityNames

        if (LOG.isTraceEnabled) {
            LOG.trace("Maintainer URL: {}, Connector URL: {}, Entity Names: {}",
                    maintainerUrl, connectorUrl, entityNames)
        }

        if (maintainerUrl != null) {
            try {
                val trustedConnectorBuilder = if (connectorUrl == null) {
                    TrustedConnectorBuilder()
                } else {
                    TrustedConnectorBuilder(connectorUrl)
                            ._hosts_(Util.asList<Host>(HostBuilder()
                            ._accessUrl_(connectorUrl.toURI()).build()))
                }
                trustedConnectorBuilder._maintainer_(maintainerUrl)
                val res = trustedConnectorBuilder._titles_(ArrayList(entityNames))
                        ._securityProfile_(securityProfile)
                        ._catalog_(catalog)
                        ._descriptions_(ArrayList(entityNames)).build()
                return res
            } catch (ex: ConstraintViolationException) {
                LOG.error("Caught ConstraintViolationException while building Connector", ex)
                return null
            } catch (ex: URISyntaxException) {
                LOG.error("Caught URISyntaxException while building Connector", ex)
                return null
            }
        } else {
            LOG.warn("Connector couldn't be built: Maintainer URL is required!")
            return null
        }
    }

    // store or update new Connector description to preferences
    // creates random connector_url if null and succeeds only if maintainerUrl and entityNames != null
    // generates RDF description from Connector object and returns building success
    override fun setConnector(profile: ConnectorProfile): Boolean {
        return if (settings != null) {
            settings?.connectorProfile = profile

            try {
                connector != null
            } catch (ex: ConstraintViolationException) {
                LOG.error("ConstraintViolationException while building Connector.", ex)
                false
            }
        } else {
            LOG.warn("Couldn't store connector object: Settings not available.")
            false
        }
    }

    override fun getConnectorAsJsonLd(): String? {
        val serializer = Serializer()
        return connector?.let { serializer.serialize(it) }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InfoModelService::class.java)
//        const val INTEGRITY_PROTECTION_AND_VERIFICATION_SUPPORT = "IntegrityProtectionAndVerificationSupport"
//        const val AUTHENTICATION_SUPPORT = "AuthenticationSupport"
//        const val SERVICE_ISOLATION_SUPPORT = "ServiceIsolationSupport"
//        const val INTEGRITY_PROTECTION_SCOPE = "IntegrityProtectionScope"
//        const val APP_EXECUTION_RESOURCES = "AppExecutionResources"
//        const val DATA_USAGE_CONTROL_SUPPORT = "DataUsageControlSupport"
//        const val AUDIT_LOGGING = "AuditLogging"
//        const val LOCAL_DATA_CONFIDENTIALITY = "LocalDataConfidentiality"
//        const val CONNECTOR_URL = "ConnectorUrl"
//        const val MAINTAINER_URL = "MaintainerUrl"
//        const val CONNECTOR_ENTITIES = "ConnectorEntities"
    }

}
