package de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.daps;

import de.fhg.aisec.ids.idscp2.drivers.default_driver_impl.keystores.PreConfiguration;
import de.fhg.aisec.ids.idscp2.drivers.interfaces.DapsDriver;
import de.fhg.aisec.ids.idscp2.error.DatException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.*;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jose4j.http.Get;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Default DAPS Driver Implementation for requesting valid dynamicAttributeToken and verifying DAT
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
public class DefaultDapsDriver implements DapsDriver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDapsDriver.class);

    private final SSLSocketFactory sslSocketFactory; //ssl socket factory can be reused
    private final X509ExtendedTrustManager trustManager; //trust manager can be reused
    private final Key privateKey; //private key can be reused
    private final String dapsUrl;
    private final String TARGET_AUDIENCE = "idsc:IDS_CONNECTORS_ALL";
    private final X509Certificate cert;

    public DefaultDapsDriver(DefaultDapsDriverConfig config) {
        this.dapsUrl = config.getDapsUrl();

        //create ssl socket factory for secure
        privateKey = PreConfiguration.getKey(
                config.getKeyStorePath(),
                config.getKeyStorePassword(),
                config.getKeyAlias(),
                config.getKeyPassword()
        );

        cert = PreConfiguration.getCertificate(
            config.getKeyStorePath(),
            config.getKeyStorePassword(),
            config.getKeyAlias());

        TrustManager[] trustManagers = PreConfiguration.getX509ExtTrustManager(
                config.getTrustStorePath(),
                config.getTrustStorePassword()
        );

        this.trustManager = (X509ExtendedTrustManager) trustManagers[0];

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Cannot init DefaultDapsDriver: {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    /**
     * Receive the signed and valid dynamic attribute token from the DAPS
     */
    @Override
    public byte[] getToken() {
        String token;

        LOG.info("Retrieving Dynamic Attribute Token from Daps ...");

        //Create connectorUUID
        // Get AKI
        //GET 2.5.29.14	SubjectKeyIdentifier / 2.5.29.35	AuthorityKeyIdentifier
        String aki_oid = Extension.authorityKeyIdentifier.getId();
        byte[] rawAuthorityKeyIdentifier = cert.getExtensionValue(aki_oid);
        ASN1OctetString akiOc = ASN1OctetString.getInstance(rawAuthorityKeyIdentifier);
        AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(akiOc.getOctets());
        byte[] authorityKeyIdentifier = aki.getKeyIdentifier();

        //GET SKI
        String ski_oid = Extension.subjectKeyIdentifier.getId();
        byte[] rawSubjectKeyIdentifier = cert.getExtensionValue(ski_oid);
        ASN1OctetString ski0c = ASN1OctetString.getInstance(rawSubjectKeyIdentifier);
        SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(ski0c.getOctets());
        byte[] subjectKeyIdentifier = ski.getKeyIdentifier();

        String aki_result = encodeHexString(authorityKeyIdentifier, true).toUpperCase();
        String ski_result = encodeHexString(subjectKeyIdentifier, true).toUpperCase();
        if (LOG.isDebugEnabled()) {
            LOG.debug("AKI: " + aki_result);
            LOG.debug("SKI: " + ski_result);
        }

        String connectorUUID = ski_result + "keyid:" + aki_result.substring(0, aki_result.length() - 1);

        if (LOG.isDebugEnabled()) {
            LOG.debug("ConnectorUUID: " + connectorUUID);
            LOG.debug("Retrieving Dynamic Attribute Token...");
        }

        //create signed JWT
        String jwt =
                Jwts.builder()
                        .setIssuer(connectorUUID)
                        .setSubject(connectorUUID)
                        .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
                        .claim("@type", "ids:DatRequestToken")
                        .setExpiration(Date.from(Instant.now().plusSeconds(86400)))
                        .setIssuedAt(Date.from(Instant.now()))
                        .setNotBefore(Date.from(Instant.now()))
                        .setAudience(TARGET_AUDIENCE)
                        .signWith(privateKey, SignatureAlgorithm.RS256)
                        .compact();

        //build http client and request for DAPS
        RequestBody formBody =
                new FormBody.Builder()
                        .add("grant_type", "client_credentials")
                        .add(
                                "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                        .add("client_assertion", jwt)
                        .add("scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
                        .build();

        OkHttpClient client =
                new OkHttpClient.Builder()
                        .sslSocketFactory(sslSocketFactory, trustManager)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

        Request request =
                new Request.Builder()
                        .url(dapsUrl.concat("/v2/token"))
                        .post(formBody)
                        .build();

        try {
            //get http response from DAPS
            if (LOG.isDebugEnabled()) {
                LOG.debug("Acquire DAT from {}", dapsUrl);
            }
            Response response = client.newCall(request).execute();

            //check for valid response
            if (!response.isSuccessful()) {
                throw new DatException("Received non-200 http response: " + response.code());
            }

            if (response.body() == null) {
                throw new DatException("Received empty DAPS response");
            }

            JSONObject json = new JSONObject(response.body().string());
            if (json.has("access_token")) {
                token = json.getString("access_token");
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received DAT from DAPS: {}", token);
                }
            } else if (json.has("error")) {
                throw new DatException("DAPS reported error: " + json.getString("error"));
            } else {
                throw new DatException("DAPS response does not contain \"access_token\" or \"error\" field.");
            }

            verifyToken(token.getBytes(StandardCharsets.UTF_8), null);
            return token.getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatException("Error whilst retrieving DAT", e);
        }
    }

    /**
     * Verify a given dynamic attribute token
     *
     * If the security requirements is not null and an instance of the SecurityRequirements class
     * the method will also check the provided security attributes of the connector that belongs
     * to the provided DAT
     *
     * @return The number of seconds this DAT is valid
     */
    @Override
    public long verifyToken(byte[] dat, Object securityRequirements) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Verifying dynamic attribute token...");
        }

        // Get JsonWebKey JWK from JsonWebKeyStore JWKS using DAPS JWKS endpoint
        HttpsJwks httpsJwks = new HttpsJwks(dapsUrl.concat("/.well-known/jwks.json"));
        Get getInstance = new Get();
        getInstance.setSslSocketFactory(sslSocketFactory);
        httpsJwks.setSimpleHttpGet(getInstance);

        // create new jwks key resolver, selects jwk based on key ID in jwt header
        HttpsJwksVerificationKeyResolver jwksKeyResolver
                = new HttpsJwksVerificationKeyResolver(httpsJwks);

        //create validation requirements
        JwtConsumer jwtConsumer =
                new JwtConsumerBuilder()
                        .setRequireExpirationTime()         // has expiration time
                        .setAllowedClockSkewInSeconds(30)   // leeway in validation time
                        .setRequireSubject()                // has subject
                        .setExpectedAudience(true, "IDS_Connector", TARGET_AUDIENCE)
                        .setExpectedIssuer(dapsUrl)         // e.g. https://daps.aisec.fraunhofer.de
                        .setVerificationKeyResolver(jwksKeyResolver) //get decryption key from jwks
                        .setJweAlgorithmConstraints(
                                new AlgorithmConstraints(
                                        ConstraintType.WHITELIST,
                                        AlgorithmIdentifiers.RSA_USING_SHA256
                                )
                        )
                        .build();

        long validityTime;
        JwtClaims claims;

        try {
            claims = jwtConsumer.processToClaims(new String(dat, StandardCharsets.UTF_8));
            NumericDate expTime = claims.getExpirationTime();
            validityTime = expTime.getValue() - NumericDate.now().getValue();
        } catch (Exception e) {
            throw new DatException("Error during claims processing", e);
        }

        //check security requirements
        if (securityRequirements != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Validate security attributes");
            }

            if (!(securityRequirements instanceof SecurityRequirements)) {
                throw new DatException("Invalid security requirements format. Expected " +
                        SecurityRequirements.class.getName());
            }
            SecurityRequirements secRequirements = (SecurityRequirements) securityRequirements;
            final var securityLevel =
                    parseSecurityRequirements(claims.toJson()).getRequiredSecurityLevel();

            if (securityLevel == null) {
                throw new DatException("No security profile provided");
            }

            switch (secRequirements.getRequiredSecurityLevel()) {
                case "idsc:BASE_CONNECTOR_SECURITY_PROFILE":
                    if (securityLevel.equals("idsc:BASE_CONNECTOR_SECURITY_PROFILE")) {
                        break;
                    }
                case "idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE":
                    if (securityLevel.equals("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE")) {
                        break;
                    }
                case "idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE":
                    if (securityLevel.equals("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")) {
                        break;
                    }
                default:
                    throw new DatException(
                            "Client does not support any valid trust profile: Required: "
                                    + secRequirements.getRequiredSecurityLevel()
                                    + " given: "
                                    + securityLevel);
            }
        }

        LOG.debug("DAT is valid");
        return validityTime;
    }

    @NonNull
    private SecurityRequirements parseSecurityRequirements(String dat) {
        JSONObject asJson = new JSONObject(dat);

        if (!asJson.has("securityProfile")) {
            throw new DatException("DAT does not contain securityProfile");
        }

        return new SecurityRequirements.Builder()
                .setRequiredSecurityLevel(asJson.getString("securityProfile"))
                .build();
    }

    /**
     * Convert byte to hexadecimal chars without any dependencies to libraries.
     * @param num Byte to get hexadecimal representation for
     * @return The hexadecimal representation of the given byte value
     */
    private char[] byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return hexDigits;
    }

    /**
     * Lookup table for encodeHexString()
     */
    private final HashMap<Byte, char[]> hexLookup = new HashMap<>();
    /**
     * Encode a byte array to a hex string
     * @param byteArray Byte array to get hexadecimal representation for
     * @return Hexadecimal representation of the given bytes
     */
    private String encodeHexString(byte[] byteArray, @SuppressWarnings("SameParameterValue") boolean beautify) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(hexLookup.computeIfAbsent(b, this::byteToHex));
            if (beautify) {
                sb.append(':');
            }
        }
        return sb.toString();
    }
}
