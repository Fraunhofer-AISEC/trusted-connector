/*-
 * ========================LICENSE_START=================================
 * TokenManager
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.tokenmanager;

import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import io.jsonwebtoken.JwtBuilder;
import org.jose4j.http.Get;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.*;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.fhg.aisec.ids.api.tokenm.TokenManager;
import de.fhg.aisec.ids.api.settings.Settings;

import java.util.Arrays;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;
import java.time.Instant;
import io.jsonwebtoken.*;
import org.json.*;

//import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;


import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.*;


/**
 * Manages Dynamic Attribute Tokens.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 *
 */
@Component(immediate=true, name="ids-tokenmanager")
public class TokenManagerService implements TokenManager {
    private static final Logger LOG = LoggerFactory.getLogger(TokenManagerService.class);
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private Settings settings = null;
    private InfoModel infoModelMgr = null;
    /*
     * The following block subscribes this component to the Settings Service
     */
    @Reference(
            name = "config.service",
            service = Settings.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindSettingsService"
    )
    public void bindSettingsService(Settings s) {
        LOG.info("Bound to configuration service");
        settings = s;
    }

    /*
     * The following block subscribes this component to the InfoModel Service
     */
    @Reference(
            name = "infomodel.service",
            service = InfoModel.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindInfoModelService"
    )

    public void bindInfoModelService(InfoModel i) {
        LOG.info("Bound to infomodel service");
        infoModelMgr = i;
    }

    @SuppressWarnings("unused")
    public void unbindSettingsService(Settings s) {
        settings = null;
    }

    @SuppressWarnings("unused")
    public void unbindInfoModelService(InfoModel i) {
        infoModelMgr = null;
    }

    /**
     * Method to aquire a Dynamic Attribute Token (DAT) from a Dynamic Attribute Provisioning Service (DAPS)
     *  @param targetDirectory - The directory the keystore resides in
     * @param dapsUrl - the token aquiry URL (e.g., http://daps.aisec.fraunhofer.de/token
     * @param keyStoreName - name of the keystore file (e.g., server-keystore.jks)
     * @param keyStorePassword - password of keystore
     * @param keystoreAliasName - alias of the connector's key entry. For default keystores with only one entry, this is '1'
     * @param trustStoreName - name of the truststore file
     * @param connectorUUID - The UUID used to register the connector at the DAPS. Should be replaced by working code that does this automatically
     */
    public boolean acquireToken(Path targetDirectory, String dapsUrl, String keyStoreName, String keyStorePassword, String keystoreAliasName, String trustStoreName, String connectorUUID) {

        String targetAudience = "IDS_Connector";
        //TODO: This is a bug in the DAPS. Audience should be not set to a default value "IDS_Connector"
        String issuer = connectorUUID;
        String subject = connectorUUID;
        String dynamicAttributeToken = "INVALID_TOKEN";
        boolean tokenVerified = false;

        //Try clause for setup phase (loading keys, building trust manager)
        try {
            InputStream jksKeyStoreInputStream = Files.newInputStream(targetDirectory.resolve(keyStoreName));
            InputStream jksTrustStoreInputStream = Files.newInputStream(targetDirectory.resolve(trustStoreName));

            KeyStore keystore = KeyStore.getInstance("JKS");
            KeyStore trustManagerKeyStore = KeyStore.getInstance("JKS");

            LOG.info("Loading key store: " + keyStoreName);
            LOG.info("Loading trus store: " + trustStoreName);
            keystore.load(jksKeyStoreInputStream, keyStorePassword.toCharArray());
            trustManagerKeyStore.load(jksTrustStoreInputStream,keyStorePassword.toCharArray());
            java.security.cert.Certificate[] certs =  trustManagerKeyStore.getCertificateChain("ca");
            LOG.info("Cert chain: " + Arrays.toString( certs ));

            LOG.info("LOADED CA CERT: " + trustManagerKeyStore.getCertificate("ca"));
            jksKeyStoreInputStream.close();
            jksTrustStoreInputStream.close();

            // get private key
            Key privKey = (PrivateKey) keystore.getKey(keystoreAliasName, keyStorePassword.toCharArray());
            // Get certificate of public key
            X509Certificate cert = (X509Certificate)keystore.getCertificate(keystoreAliasName);

            TrustManager[] trustManagers;
            SSLSocketFactory sslSocketFactory;
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustManagerKeyStore);
                trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagers, null);
                sslSocketFactory = sslContext.getSocketFactory();
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }


            LOG.info("Retrieving Dynamic Attribute Token...");

            //create signed JWT (JWS)
            //Create expiry date one day (86400 seconds) from now
            Date expiryDate = Date.from(Instant.now().plusSeconds(86400));
            JwtBuilder jwtb = Jwts.builder()
                    .setIssuer(issuer)
                    .setSubject(subject)
                    .setExpiration(expiryDate)
                    .setIssuedAt(Date.from(Instant.now()))
                    .setAudience(targetAudience)
                    .setNotBefore(Date.from(Instant.now()));
            LOG.info("\tCertificate Subject: " + cert.getSubjectDN());
            String jws = jwtb.signWith(privKey, SignatureAlgorithm.RS256).compact();

            //build form body to embed client assertion into post request
            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .add("client_assertion", jws)
                    .add("scope", "ids_connector")
                    .build();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            //configure client with trust manager
            OkHttpClient client = builder
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager)trustManagers[0])
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(dapsUrl+"/token").post(formBody).build();
            Response jwtresponse = client.newCall(request).execute();

            String responseBody = jwtresponse.body().string();

            if (!jwtresponse.isSuccessful())
                throw new IOException("Unexpected code " + jwtresponse);

            JSONObject jsonObject = new JSONObject(responseBody);
            dynamicAttributeToken = jsonObject.getString("access_token");


            LOG.info("Dynamic Attribute Token: " + dynamicAttributeToken);

            tokenVerified = verifyJWT(dynamicAttributeToken, targetAudience, sslSocketFactory, dapsUrl);

        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException e) {
            LOG.error("Cannot acquire token:", e);
        } catch (IOException e) {
            LOG.error("Error retrieving token:", e);
        } catch (Exception e) {
            LOG.error("Something else went wrong:", e);
        }

        if(tokenVerified)
            infoModelMgr.setDynamicAttributeToken(dynamicAttributeToken);
        return tokenVerified;

    }

    private boolean verifyJWT(String dynamicAttributeToken, String targetAudience, SSLSocketFactory sslSocketFactory, String dapsUrl) {
        boolean verificationSucceeded = false;
        try
        {
            // The HttpsJwks retrieves and caches keys from a the given HTTPS JWKS endpoint.
            // Because it retains the JWKs after fetching them, it can and should be reused
            // to improve efficiency by reducing the number of outbound calls the the endpoint.
            HttpsJwks httpsJkws = new HttpsJwks(dapsUrl + "/.well-known/jwks.json");
            Get getInstance = new Get();
            getInstance.setSslSocketFactory(sslSocketFactory);
            httpsJkws.setSimpleHttpGet(getInstance);

            // The HttpsJwksVerificationKeyResolver uses JWKs obtained from the HttpsJwks and will select the
            // most appropriate one to use for verification based on the Key ID and other factors provided
            // in the header of the JWS/JWT.
            HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);

            // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
            // be used to validate and process the JWT.
            // The specific validation requirements for a JWT are context dependent, however,
            // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
            // and audience that identifies your system as the intended recipient.
            // If the JWT is encrypted too, you need only provide a decryption key or
            // decryption key resolver to the builder.
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime() // the JWT must have an expiration time
                    .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                    .setRequireSubject() // the JWT must have a subject claim
                    .setExpectedIssuer("https://daps.aisec.fraunhofer.de") // whom the JWT needs to have been issued by
                    .setExpectedAudience(targetAudience) // to whom the JWT is intended for
                    .setVerificationKeyResolver(httpsJwksKeyResolver)
                    .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                            new org.jose4j.jwa.AlgorithmConstraints(org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
                                    AlgorithmIdentifiers.RSA_USING_SHA256))
                    .build(); // create the JwtConsumer instance


            LOG.info("Verifying JWT...");
            //  Validate the JWT and process it to the Claims
            JwtClaims jwtClaims = jwtConsumer.processToClaims(dynamicAttributeToken);
            LOG.info("JWT validation succeeded! " + jwtClaims);
            verificationSucceeded = true;
        }
        catch (InvalidJwtException e)
        {
            // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
            // Hopefully with meaningful explanations(s) about what went wrong.
            LOG.info("Invalid JWT! " + e);

            // Programmatic access to (some) specific reasons for JWT invalidity is also possible
            // should you want different error handling behavior for certain conditions.

            // Whether or not the JWT has expired being one common reason for invalidity
            if (e.hasExpired())
            {
                try {
                    LOG.info("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
                } catch (MalformedClaimException e1) {
                    LOG.info("Malformed claim");
                }
            }

            // Or maybe the audience was invalid
            if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID))
            {
                try {
                    LOG.info("JWT had wrong audience: " + e.getJwtContext().getJwtClaims().getAudience());
                } catch (MalformedClaimException e1) {
                    LOG.info("Malformed claim");
                }
            }
        }
        return verificationSucceeded;
    }


    @Activate
    public void run() {
        LOG.info("Token renewal triggered.");
        try {
            ConnectorConfig config = settings.getConnectorConfig();
            acquireToken(FileSystems.getDefault().getPath("etc"), config.getDapsUrl(), config.getKeystoreName(), config.getKeystorePassword(), config.getKeystoreAliasName(),config.getTruststoreName() ,config.getConnectorUUID());

        } catch (Exception e) {
            LOG.error("Token renewal failed", e);
        }
    }

}