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

import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import io.jsonwebtoken.JwtBuilder;
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

import java.util.HashMap;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import io.jsonwebtoken.*;

//import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;


import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import okio.*;

import javax.net.ssl.*;

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

    @SuppressWarnings("unused")
    public void unbindSettingsService(Settings s) {
        settings = null;
    }

    /**
     * Method to aquire a Dynamic Attribute Token (DAT) from a Dynamic Attribute Provisioning Service (DAPS)
     *
     * @param targetDirectory - The directory the keystore resides in
     * @param dapsUrl - the token aquiry URL (e.g., http://daps.aisec.fraunhofer.de/token
     * @param keyStoreName - name of the keystore file (e.g., server-keystore.jks)
     * @param keyStorePassword - password of keystore
     * @param keystoreAliasName - alias of the connector's key entry. For default keystores with only one entry, this is '1'
     * @param connectorUUID - The UUID used to register the connector at the DAPS. Should be replaced by working code that does this automatically
     */
    public void acquireToken(Path targetDirectory, String dapsUrl, String keyStoreName, String keyStorePassword, String keystoreAliasName, String connectorUUID) {

        LOG.info("Resolving path for keystore: " + keyStoreName);
        LOG.info("Path to resolve: " + FileSystems.getDefault().getPath("etc").toString());
        String targetAudience = "api.Audience";
        String issuer = connectorUUID;
        String subject = connectorUUID;

        try (InputStream jksInputStream = Files.newInputStream(targetDirectory.resolve(keyStoreName))) {
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(jksInputStream, keyStorePassword.toCharArray());
            // get private key
            Key privKey = (PrivateKey) store.getKey(keystoreAliasName, keyStorePassword.toCharArray());
            // Get certificate of public key
            X509Certificate cert = (X509Certificate)store.getCertificate(keystoreAliasName);
            LOG.info("Private key loaded. Retrieving Dynamic Attribute Token...");

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


            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                    .add("client_assertion", jws)
                    .add("scope", "ids_connector")
                    .build();


            /**
             * Trust Manager
             */
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient client = builder
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(dapsUrl+"/token").post(formBody).build();
            LOG.info("Request looks like this: " + bodyToString(request));
            Response jwtresponse = client.newCall(request).execute();

            LOG.info("Response: " + jwtresponse.toString());
            LOG.info("Body: " + jwtresponse.body().string());
            LOG.info("Message: " + jwtresponse.message());

            if (!jwtresponse.isSuccessful())
                throw new IOException("Unexpected code " + jwtresponse);

            // The HttpsJwks retrieves and caches keys from a the given HTTPS JWKS endpoint.
            // Because it retains the JWKs after fetching them, it can and should be reused
            // to improve efficiency by reducing the number of outbound calls the the endpoint.
            HttpsJwks httpsJkws = new HttpsJwks(dapsUrl + "/.well-known/jwks.json");

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
                    .setExpectedIssuer(issuer) // whom the JWT needs to have been issued by
                    .setExpectedAudience(targetAudience) // to whom the JWT is intended for
                    .setVerificationKeyResolver(httpsJwksKeyResolver)
                    .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                            new org.jose4j.jwa.AlgorithmConstraints(org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
                                    AlgorithmIdentifiers.RSA_USING_SHA256))
                    .build(); // create the JwtConsumer instance

            //HashMap jwtMap = new ObjectMapper().readValue(jwtresponse.body().string(), HashMap.class);

            String accessToken = "";//(String) jwtMap.get("access_token");

            try
            {
                LOG.info("Verifying JWT...");
                //  Validate the JWT and process it to the Claims
                JwtClaims jwtClaims = jwtConsumer.processToClaims(accessToken);
                LOG.info("JWT validation succeeded! " + jwtClaims);
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
                    LOG.info("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
                }

                // Or maybe the audience was invalid
                if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID))
                {
                    LOG.info("JWT had wrong audience: " + e.getJwtContext().getJwtClaims().getAudience());
                }
            }


        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException | MalformedClaimException e) {
            LOG.error("Cannot acquire token:", e);
        } catch (IOException e) {
            LOG.error("Cannot load key:", e);
        }

        //TODO: Get Token, return it


    }

    private static String bodyToString(final Request request){

        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    @Activate
    public void run() {
        LOG.info("Token renewal triggered.");
        try {
            ConnectorConfig config = settings.getConnectorConfig();
            acquireToken(FileSystems.getDefault().getPath("etc"), config.getDapsUrl(), config.getKeystoreName(), config.getKeystorePassword(), config.getKeystoreAliasName(), config.getConnectorUUID());
        } catch (Exception e) {
            LOG.error("Token renewal failed", e);
        }
    }

}