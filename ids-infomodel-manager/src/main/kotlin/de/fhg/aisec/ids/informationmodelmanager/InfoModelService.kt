package de.fhg.aisec.ids.informationmodelmanager

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.informationmodelmanager.deserializer.CustomObjectMapper
import de.fraunhofer.iais.eis.*
import de.fraunhofer.iais.eis.util.ConstraintViolationException
import de.fraunhofer.iais.eis.util.PlainLiteral
import de.fraunhofer.iais.eis.util.Util
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ReferenceCardinality
import org.osgi.service.prefs.Preferences
import org.osgi.service.prefs.PreferencesService
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*


/**
 * IDS Info Model Manager.
 */
@Component(name = "ids-infomodel-manager", immediate = true)
class InfoModelService : InfoModel {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var preferencesService: PreferencesService? = null
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var connectionManager: ConnectionManager? = null

    private val connectorModel: Preferences? by lazy {
        preferencesService?.getUserPreferences(CONNECTOR_MODEL)
    }

    /**
     * Build Connector Entity Names from preferences
     */
    private val connectorEntityNames: List<PlainLiteral>
        get() {
            if (connectorModel != null) {
                val typeRef = object : TypeReference<List<PlainLiteral>>() {}
                try {
                    return mapper.readValue(connectorModel?.get(CONNECTOR_ENTITIES, null), typeRef)
                } catch (e: Exception) {
                    LOG.warn(e.message, e)
                }
            } else {
                LOG.warn("Model preferences not available")
            }
            return emptyList()
        }

    /**
     * Build current endpoints as given by connectionManager
     * will not be stored in preferences, but freshly loaded each time.
     * Multiple names for one connector allowed
     */
    private fun getEndpoints(): List<Endpoint> {
//        Endpoint eP;
//        List<Endpoint> ePs = new ArrayList<>();
//
//        if (connectionManager!=null) {
//
//            List<IDSCPServerEndpoint> sePs = connectionManager.listAvailableEndpoints();
//
//            for (IDSCPServerEndpoint tempEP : sePs) {
//                // http://industrialdataspace.org/connector1/endpoint1
//                try {
//                    eP = new EndpointBuilder()
//                            .entityNames(Arrays.asList(new PlainLiteral(tempEP.getEndpointIdentifier()))).build();
//                    ePs.add(eP);
//                } catch (ConstraintViolationException ex) {
//                    LOG.error("Caught ConstraintViolationException while building Endpoints", ex);
//                }
//            }
//
//            return ePs
//        }
        return emptyList()
    }

    /**
     * Build Security Profile from preferences
     * defaults to "NONE" for all attributes in case nothing has been stored
     * @return
     */
    private val securityProfile: SecurityProfile?
        get() {
            if (connectorModel != null) {
                val securityProfileBuilder: SecurityProfileBuilder
                try {
                    val psp = connectorModel?.get("basedOn", null)?.let {
                        // For some reason, PredefinedSecurityProfile.valueOf() doesn't work, so we resort to
                        // PredefinedSecurityProfile.deserialize() with temporary JsonNode
                        PredefinedSecurityProfile.deserialize(mapper.createObjectNode().put("@id", it))
                    }
                    securityProfileBuilder = connectorModel?.get("sPID", null)?.let {
                        SecurityProfileBuilder(URL(it))
                    } ?: SecurityProfileBuilder()
                    return securityProfileBuilder._basedOn_(psp)
                            ._integrityProtectionAndVerificationSupport_(
                                    IntegrityProtectionAndVerificationSupport
                                            .valueOf(connectorModel?.get(INTEGRITY_PROTECTION_AND_VERIFICATION_SUPPORT, null)
                                                    ?: IntegrityProtectionAndVerificationSupport.NO_INTEGRITY_PROTECTION.name))
                            ._authenticationSupport_(
                                    AuthenticationSupport.valueOf(connectorModel?.get(AUTHENTICATION_SUPPORT, null)
                                            ?: AuthenticationSupport.NO_AUTHENTICATION.name))
                            ._serviceIsolationSupport_(
                                    ServiceIsolationSupport.valueOf(connectorModel?.get(SERVICE_ISOLATION_SUPPORT, null)
                                            ?: ServiceIsolationSupport.NO_SERVICE_ISOLATION.name))
                            ._integrityProtectionScope_(
                                    IntegrityProtectionScope.valueOf(connectorModel?.get(INTEGRITY_PROTECTION_SCOPE, null)
                                            ?: IntegrityProtectionScope.UNKNOWN_INTEGRITY_PROTECTION_SCOPE.name))
                            ._appExecutionResources_(
                                    AppExecutionResources.valueOf(connectorModel?.get(APP_EXECUTION_RESOURCES, null)
                                            ?: AppExecutionResources.NO_APP_EXECUTION.name))
                            ._dataUsageControlSupport_(
                                    DataUsageControlSupport.valueOf(connectorModel?.get(DATA_USAGE_CONTROL_SUPPORT, null)
                                            ?: DataUsageControlSupport.NO_USAGE_CONTROL.name))
                            ._auditLogging_(AuditLogging
                                    .valueOf(connectorModel?.get(AUDIT_LOGGING, AuditLogging.NO_AUDIT_LOGGING.name)!!))
                            ._localDataConfidentiality_(
                                    LocalDataConfidentiality.valueOf(connectorModel?.get(LOCAL_DATA_CONFIDENTIALITY, null)
                                            ?: LocalDataConfidentiality.NO_CONFIDENTIALITY.name))
                            .build()
                } catch (ex: ConstraintViolationException) {
                    LOG.error(
                            "ConstraintViolationException while building Security profile from preferences.", ex)
                } catch (ex: MalformedURLException) {
                    LOG.error(
                            "MalformedURLException while building Security profile from preferences.", ex)
                }
            } else {
                LOG.warn("Model preferences not available")
            }
            return null
        }

    private fun setPreference(key: String, value: String, p: Preferences?) {
        if (p != null) {
            p.put(key, value)
        } else {
            LOG.error("No Preferences available.")
        }
    }

    /**
     * Retrieve URL-String from preferences by key and build URL
     * (ConnectorURL: CONNECTOR_URL, OperatorURL: MAINTAINER_URL)
     * @param key
     * @return
     */
    private fun getURL(key: String): URL? {
        if (connectorModel != null) {
            try {
                return URL(connectorModel?.get(key, null))
            } catch (ex: MalformedURLException) {
                LOG.warn("Caught MalformedURLException while building URL from preferences", ex)
            }
        } else {
            LOG.error("No Preferences available for building URL.")
        }
        return null
    }

    /**
     * Generates RDF description based on Connector object and
     * stores it to preferences on each Connector update
     * @param c
     * @return
     */
    private fun generateRDF(c: Connector): Boolean {
        return if (connectorModel != null) {
            val rdf = c.toRdf()
            connectorModel?.put("infomodel", rdf)
            LOG.debug("Generated RDF description: $rdf")
            true
        } else {
            LOG.debug("Couldn't get Preferences for generating RDF.")
            false
        }
    }

    /**
     * get RDF description from preferences
     * @return
     */
    override fun getRDF(): String? {
        if (connectorModel != null) {
            return connectorModel?.get("infomodel", "")
        } else {
            LOG.error("Couldn't get Connector description.")
        }
        return null
    }

    // creates and returns Connector object based on stored preferences
    // returns random connector_url if none is stored in preferences
    // op_url and entityNames can not be null
    override fun getConnector(): Connector? {
        val securityProfile = securityProfile
        val connectorUrl = getURL(CONNECTOR_URL)
        val maintainerUrl = getURL(MAINTAINER_URL)
        val entityNames = connectorEntityNames

        LOG.debug("Maintainer URL: {}, Connector URL: {}, Entity Names: {}", maintainerUrl, connectorUrl, entityNames)

        if (maintainerUrl != null) {
            try {
                val trustedConnectorBuilder = (if (connectorUrl == null)
                    TrustedConnectorBuilder()
                else
                    TrustedConnectorBuilder(connectorUrl))._maintainer_(maintainerUrl)
                if (connectorUrl != null) {
                    trustedConnectorBuilder
                            ._hosts_(Util.asList<Host>(HostBuilder()
                                    ._accessUrl_(connectorUrl.toURI()).build()))
                }
                return trustedConnectorBuilder._titles_(ArrayList(entityNames))
                        ._securityProfile_(securityProfile)
                        ._descriptions_(ArrayList(entityNames)).build()
            } catch (ex: ConstraintViolationException) {
                LOG.error("Caught ConstraintViolationException while building Connector", ex)
                return null
            } catch (ex: URISyntaxException) {
                LOG.error("Caught ConstraintViolationException while building Connector", ex)
                return null
            }
        } else {
            LOG.warn("Connector couldn't be built due to null objects.")
            return null
        }
    }

    // store or update new Connector description to preferences
    // creates random connector_url if null and succeeds only if maintainerUrl and entityNames != null
    // generates RDF description from Connector object and returns building success
    override fun setConnector(connUrl: URL?, maintainerUrl: URL?, entityNames: List<PlainLiteral>?,
                              profile: SecurityProfile?): Boolean {
        if (connectorModel != null) {
            // Set Connector URL ---> allowed to be empty from model
            if (connUrl != null) {
                setPreference(CONNECTOR_URL, connUrl.toString(), connectorModel)
            }

            // Set Operator URL
            if (maintainerUrl != null) {
                setPreference(MAINTAINER_URL, maintainerUrl.toString(), connectorModel)
            } else {
                LOG.error("Operator URL must not be empty!")
                return false
            }

            // Set Entity Names
            if (entityNames != null && !entityNames.isEmpty()) {
                try {
                    val connectorEntities = mapper.writeValueAsString(entityNames)
                    setPreference(CONNECTOR_ENTITIES, connectorEntities, connectorModel)
                    LOG.trace("Stored Connector Entities: {}", connectorEntities)
                } catch (e: JsonProcessingException) {
                    LOG.error(e.message, e)
                }

            } else {
                LOG.error("Entity Names can not be empty.")
                return false
            }

            // Set SecurityProfile -- if null, will return "NONE" automatically
            if (profile != null) {
                if (profile.basedOn != null) {
                    setPreference("basedOn", profile.basedOn.toString(), connectorModel)
                }
                setPreference("sPID", profile.id.toString(), connectorModel)
                setPreference(INTEGRITY_PROTECTION_AND_VERIFICATION_SUPPORT,
                        profile.integrityProtectionAndVerificationSupport.name, connectorModel)
                setPreference(AUTHENTICATION_SUPPORT, profile.authenticationSupport.name,
                        connectorModel)
                setPreference(SERVICE_ISOLATION_SUPPORT, profile.serviceIsolationSupport.name,
                        connectorModel)
                setPreference(INTEGRITY_PROTECTION_SCOPE,
                        profile.integrityProtectionScope.name, connectorModel)
                setPreference(APP_EXECUTION_RESOURCES, profile.appExecutionResources.name,
                        connectorModel)
                setPreference(DATA_USAGE_CONTROL_SUPPORT, profile.dataUsageControlSupport.name,
                        connectorModel)
                setPreference(AUDIT_LOGGING, profile.auditLogging.name, connectorModel)
                setPreference(LOCAL_DATA_CONFIDENTIALITY,
                        profile.localDataConfidentiality.name, connectorModel)
            }

            return try {
                if (connUrl != null) {
                    generateRDF(TrustedConnectorBuilder(connUrl)._maintainer_(maintainerUrl)
                                    ._securityProfile_(profile).build())
                } else {
                    generateRDF(TrustedConnectorBuilder()._maintainer_(maintainerUrl)
                                    ._securityProfile_(profile).build())
                }
            } catch (ex: ConstraintViolationException) {
                LOG.error("ConstraintViolationException while building Connector to generate RDF description.", ex)
                false
            }
        } else {
            LOG.debug("Couldn't load preferences to store connector object.")
            return false
        }
    }

    override fun setConnector(connUrl: URL, opUrl: URL, entityNames: List<PlainLiteral>): Boolean {
        return setConnector(connUrl, opUrl, entityNames, null)
    }

    override fun setConnector(opUrl: URL, entityNames: List<PlainLiteral>): Boolean {
        return setConnector(null, opUrl, entityNames, null)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InfoModelService::class.java)
        private val mapper: ObjectMapper by lazy { CustomObjectMapper() }
        private const val CONNECTOR_MODEL = "ids.model"
        const val INTEGRITY_PROTECTION_AND_VERIFICATION_SUPPORT = "IntegrityProtectionAndVerificationSupport"
        const val AUTHENTICATION_SUPPORT = "AuthenticationSupport"
        const val SERVICE_ISOLATION_SUPPORT = "ServiceIsolationSupport"
        const val INTEGRITY_PROTECTION_SCOPE = "IntegrityProtectionScope"
        const val APP_EXECUTION_RESOURCES = "AppExecutionResources"
        const val DATA_USAGE_CONTROL_SUPPORT = "DataUsageControlSupport"
        const val AUDIT_LOGGING = "AuditLogging"
        const val LOCAL_DATA_CONFIDENTIALITY = "LocalDataConfidentiality"
        const val CONNECTOR_URL = "ConnectorUrl"
        const val MAINTAINER_URL = "MaintainerUrl"
        const val CONNECTOR_ENTITIES = "ConnectorEntities"
    }

}
