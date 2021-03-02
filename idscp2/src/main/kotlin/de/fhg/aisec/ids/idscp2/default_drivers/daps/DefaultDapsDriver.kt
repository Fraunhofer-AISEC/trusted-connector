package de.fhg.aisec.ids.idscp2.default_drivers.daps

import de.fhg.aisec.ids.idscp2.default_drivers.keystores.PreConfiguration
import de.fhg.aisec.ids.idscp2.idscp_core.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.idscp_core.error.DatException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.jose4j.http.Get
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwk.HttpsJwks
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.NumericDate
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509ExtendedTrustManager

/**
 * Default DAPS Driver Implementation for requesting valid dynamicAttributeToken and verifying DAT
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
class DefaultDapsDriver(config: DefaultDapsDriverConfig) : DapsDriver {
    private var sslSocketFactory: SSLSocketFactory
    private var securityRequirements: SecurityRequirements? = config.securityRequirements
    private val trustManager: X509ExtendedTrustManager
    private val privateKey: Key = PreConfiguration.getKey(
                    config.keyStorePath,
                    config.keyStorePassword,
                    config.keyAlias,
                    config.keyPassword
            )
    private val dapsUrl: String = config.dapsUrl
    private val cert: X509Certificate = PreConfiguration.getCertificate(
            config.keyStorePath,
            config.keyStorePassword,
            config.keyAlias)  //get http response from DAPS

    init {
        //create ssl socket factory for secure
        val trustManagers = PreConfiguration.getX509ExtTrustManager(
                config.trustStorePath,
                config.trustStorePassword
        )
        trustManager = trustManagers[0] as X509ExtendedTrustManager
        sslSocketFactory = try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagers, null)
            sslContext.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            LOG.error("Cannot init DefaultDapsDriver: {}", e.toString())
            throw RuntimeException(e)
        } catch (e: KeyManagementException) {
            LOG.error("Cannot init DefaultDapsDriver: {}", e.toString())
            throw RuntimeException(e)
        }
    }

    /**
     * Receive the signed and valid dynamic attribute token from the DAPS
     *
     * @throws DatException
     */
    override val token: ByteArray
        get() {
            val token: String
            LOG.info("Retrieving Dynamic Attribute Token from Daps ...")

            //Create connectorUUID
            // Get AKI
            //GET 2.5.29.14	SubjectKeyIdentifier / 2.5.29.35	AuthorityKeyIdentifier
            val akiOid = Extension.authorityKeyIdentifier.id
            val rawAuthorityKeyIdentifier = cert.getExtensionValue(akiOid)
            val akiOc = ASN1OctetString.getInstance(rawAuthorityKeyIdentifier)
            val aki = AuthorityKeyIdentifier.getInstance(akiOc.octets)
            val authorityKeyIdentifier = aki.keyIdentifier

            //GET SKI
            val skiOid = Extension.subjectKeyIdentifier.id
            val rawSubjectKeyIdentifier = cert.getExtensionValue(skiOid)
            val ski0c = ASN1OctetString.getInstance(rawSubjectKeyIdentifier)
            val ski = SubjectKeyIdentifier.getInstance(ski0c.octets)
            val subjectKeyIdentifier = ski.keyIdentifier
            val akiResult = encodeHexString(authorityKeyIdentifier, true).toUpperCase()
            val skiResult = encodeHexString(subjectKeyIdentifier, true).toUpperCase()
            if (LOG.isDebugEnabled) {
                LOG.debug("AKI: $akiResult")
                LOG.debug("SKI: $skiResult")
            }
            val connectorUUID = skiResult + "keyid:" + akiResult.substring(0, akiResult.length - 1)
            if (LOG.isDebugEnabled) {
                LOG.debug("ConnectorUUID: $connectorUUID")
                LOG.debug("Retrieving Dynamic Attribute Token...")
            }

            //create signed JWT
            val expiration = Date.from(Instant.now().plusSeconds(86400))
            val issuedAt = Date.from(Instant.now())
            val notBefore = Date.from(Instant.now())

            val jwt = Jwts.builder()
                    .setIssuer(connectorUUID)
                    .setSubject(connectorUUID)
                    .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                    .claim("@type", "ids:DatRequestToken")
                    .setExpiration(expiration)
                    .setIssuedAt(issuedAt)
                    .setNotBefore(notBefore)
                    .setAudience(TARGET_AUDIENCE)
                    .signWith(privateKey, SignatureAlgorithm.RS256)
                    .compact()

            //build http client and request for DAPS
            val formBody: RequestBody = FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add(
                            "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .add("client_assertion", jwt)
                    .add("scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
                    .build()
            val client = OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            val request = Request.Builder()
                    .url("$dapsUrl/v2/token")
                    .post(formBody)
                    .build()
            return try {
                //get http response from DAPS
                val response = client.newCall(request).execute()

                //check for valid response
                if (!response.isSuccessful) {
                    LOG.error("Failed to request token issued with parameters: Issuer: {}, Subject: {}, " +
                            "Expiration: {}, IssuedAt: {}, NotBefore: {}, Audience: {}",
                            connectorUUID,
                            connectorUUID,
                            expiration,
                            issuedAt,
                            notBefore,
                            TARGET_AUDIENCE)
                    throw DatException("Received non-200 http response: " + response.code())
                }
                if (LOG.isDebugEnabled) {
                    LOG.debug("Acquired DAT from {}/v2/token", dapsUrl)
                }
                val json = JSONObject(response.body()?.string()
                    ?: throw DatException("Received empty DAPS response"))
                if (json.has("access_token")) {
                    token = json.getString("access_token")
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Received DAT from DAPS: {}", token)
                    }
                } else if (json.has("error")) {
                    throw DatException("DAPS reported error: " + json.getString("error"))
                } else {
                    throw DatException("DAPS response does not contain \"access_token\" or \"error\" field.")
                }
                verifyTokenSecurityAttributes(token.toByteArray(StandardCharsets.UTF_8), null)
                token.toByteArray(StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw DatException("Error whilst retrieving DAT", e)
            }
        }

    /**
     * Update the security requirements of the DAPS (This might be caused by changes in the connector configuration)
     */
    fun updateSecurityRequirements(securityRequirements: SecurityRequirements?) {
        this.securityRequirements = securityRequirements
    }

    /**
     * Public verifyToken API, used from the IDSCPv2 protocol. Security requirements are used from the DAPS config
     *
     * @return The number of seconds this DAT is valid
     * @throws DatException
     */
    override fun verifyToken(dat: ByteArray): Long {
        return verifyTokenSecurityAttributes(dat, securityRequirements)
    }

    /**
     * Verify a given dynamic attribute token, given the security attributes as parameter. This is used for IDSCPv1
     * legacy implementation and for internal purpose
     *
     * If the security requirements is not null and an instance of the SecurityRequirements class
     * the method will also check the provided security attributes of the connector that belongs
     * to the provided DAT
     *
     * @return The number of seconds this DAT is valid
     * @throws DatException
     */
    fun verifyTokenSecurityAttributes(dat: ByteArray, securityRequirements: Any?): Long {
        if (LOG.isDebugEnabled) {
            LOG.debug("Verifying dynamic attribute token...")
        }

        // Get JsonWebKey JWK from JsonWebKeyStore JWKS using DAPS JWKS endpoint
        val httpsJwks = HttpsJwks("$dapsUrl/.well-known/jwks.json")
        val getInstance = Get()
        getInstance.setSslSocketFactory(sslSocketFactory)
        httpsJwks.setSimpleHttpGet(getInstance)

        // create new jwks key resolver, selects jwk based on key ID in jwt header
        val jwksKeyResolver = HttpsJwksVerificationKeyResolver(httpsJwks)

        //create validation requirements
        val jwtConsumer = JwtConsumerBuilder()
                .setRequireExpirationTime() // has expiration time
                .setAllowedClockSkewInSeconds(30) // leeway in validation time
                .setRequireSubject() // has subject
                .setExpectedAudience(true, "IDS_Connector", TARGET_AUDIENCE)
                .setExpectedIssuer(dapsUrl) // e.g. https://daps.aisec.fraunhofer.de
                .setVerificationKeyResolver(jwksKeyResolver) //get decryption key from jwks
                .setJweAlgorithmConstraints(
                        AlgorithmConstraints(
                                AlgorithmConstraints.ConstraintType.WHITELIST,
                                AlgorithmIdentifiers.RSA_USING_SHA256
                        )
                )
                .build()
        val validityTime: Long
        val claims: JwtClaims
        try {
            claims = jwtConsumer.processToClaims(String(dat, StandardCharsets.UTF_8))
            val expTime = claims.expirationTime
            validityTime = expTime.value - NumericDate.now().value
        } catch (e: Exception) {
            throw DatException("Error during claims processing", e)
        }

        //check security requirements
        if (securityRequirements != null) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Validate security attributes")
            }
            if (securityRequirements !is SecurityRequirements) {
                throw DatException("Invalid security requirements format. Expected " +
                        SecurityRequirements::class.java.name)
            }
            val securityLevel = parseSecurityRequirements(claims.toJson()).requiredSecurityLevel
                    ?: throw DatException("No security profile provided")
            when (securityRequirements.requiredSecurityLevel) {
                "idsc:BASE_CONNECTOR_SECURITY_PROFILE" -> {
                    if (securityLevel != "idsc:BASE_CONNECTOR_SECURITY_PROFILE"
                            && securityLevel != "idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE"
                            && securityLevel != "idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE") {
                        throw DatException(
                                "Client does not support any valid trust profile: Required: "
                                        + securityRequirements.requiredSecurityLevel
                                        + " given: "
                                        + securityLevel)
                    }
                }
                "idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE" -> {
                    if (securityLevel != "idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE"
                            && securityLevel != "idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE") {
                        throw DatException(
                                "Client does not support any valid trust profile: Required: "
                                        + securityRequirements.requiredSecurityLevel
                                        + " given: "
                                        + securityLevel)
                    }
                }
                "idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE" -> {
                    if (securityLevel != "idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE") {
                        throw DatException(
                                "Client does not support any valid trust profile: Required: "
                                        + securityRequirements.requiredSecurityLevel
                                        + " given: "
                                        + securityLevel)
                    }
                }
                else -> throw DatException(
                        "Client does not support any valid trust profile: Required: "
                                + securityRequirements.requiredSecurityLevel
                                + " given: "
                                + securityLevel)
            }
        }
        LOG.debug("DAT is valid")
        return validityTime
    }

    private fun parseSecurityRequirements(dat: String): SecurityRequirements {
        val asJson = JSONObject(dat)
        if (!asJson.has("securityProfile")) {
            throw DatException("DAT does not contain securityProfile")
        }
        return SecurityRequirements.Builder()
                .setRequiredSecurityLevel(asJson.getString("securityProfile"))
                .build()
    }

    /**
     * Convert byte to hexadecimal chars without any dependencies to libraries.
     * @param num Byte to get hexadecimal representation for
     * @return The hexadecimal representation of the given byte value
     */
    private fun byteToHex(num: Int): CharArray {
        val hexDigits = CharArray(2)
        hexDigits[0] = Character.forDigit(num shr 4 and 0xF, 16)
        hexDigits[1] = Character.forDigit(num and 0xF, 16)
        return hexDigits
    }

    /**
     * Lookup table for encodeHexString()
     */
    private val hexLookup = HashMap<Byte, CharArray>()

    /**
     * Encode a byte array to a hex string
     * @param byteArray Byte array to get hexadecimal representation for
     * @return Hexadecimal representation of the given bytes
     */
    private fun encodeHexString(byteArray: ByteArray, beautify: Boolean): String {
        val sb = StringBuilder()
        for (b in byteArray) {
            sb.append(hexLookup.computeIfAbsent(b) { num: Byte -> byteToHex(num.toInt()) })
            if (beautify) {
                sb.append(':')
            }
        }
        return sb.toString()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DefaultDapsDriver::class.java)
        private const val TARGET_AUDIENCE = "idsc:IDS_CONNECTORS_ALL"
    }
}