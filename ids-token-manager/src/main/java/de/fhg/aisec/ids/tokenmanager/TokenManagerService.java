/*-
 * ========================LICENSE_START=================================
 * ids-token-manager
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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

import de.fhg.aisec.ids.api.settings.ConnectionSettings;
import de.fhg.aisec.ids.api.settings.ConnectorConfig;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.api.tokenm.DatException;
import de.fhg.aisec.ids.api.tokenm.TokenManager;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.*;
import org.jose4j.http.Get;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Manages Dynamic Attribute Tokens.
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-tokenmanager")
public class TokenManagerService implements TokenManager {
  private static final Logger LOG = LoggerFactory.getLogger(TokenManagerService.class);

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  private Settings settings = null;
  private SSLSocketFactory sslSocketFactory = null;

  /**
   * Method to aquire a Dynamic Attribute Token (DAT) from a Dynamic Attribute Provisioning Service
   * (DAPS)
   *
   * @param targetDirectory   The directory the keystore resides in
   * @param dapsUrl           The token aquiry URL (e.g., http://daps.aisec.fraunhofer.de/token
   * @param keyStoreName      Name of the keystore file (e.g., server-keystore.jks)
   * @param keyStorePassword  Password of keystore
   * @param keystoreAliasName Alias of the connector's key entry. For default keystores with only
   *                          one entry, this is '1'
   * @param trustStoreName    Name of the truststore file
   * @param connectorUUID     The UUID used to register the connector at the DAPS. Should be replaced by
   *                          working code that does this automatically
   */
  @Override
  public Map<String, Object> acquireToken(
      Path targetDirectory,
      String dapsUrl,
      String keyStoreName,
      String keyStorePassword,
      String keystoreAliasName,
      String trustStoreName,
      String connectorUUID) {

    String dynamicAttributeToken = "INVALID_TOKEN";
    String targetAudience = "IDS_Connector";

    Map<String, Object> jwtClaims = null;

    // Try clause for setup phase (loading keys, building trust manager)
    try {
      InputStream jksKeyStoreInputStream =
          Files.newInputStream(targetDirectory.resolve(keyStoreName));
      InputStream jksTrustStoreInputStream =
          Files.newInputStream(targetDirectory.resolve(trustStoreName));

      KeyStore keystore = KeyStore.getInstance("JKS");
      KeyStore trustManagerKeyStore = KeyStore.getInstance("JKS");

      LOG.info("Loading key store: " + keyStoreName);
      LOG.info("Loading trust store: " + trustStoreName);
      keystore.load(jksKeyStoreInputStream, keyStorePassword.toCharArray());
      trustManagerKeyStore.load(jksTrustStoreInputStream, keyStorePassword.toCharArray());
      java.security.cert.Certificate[] certs = trustManagerKeyStore.getCertificateChain("ca");
      LOG.info("Cert chain: " + Arrays.toString(certs));

      LOG.info("LOADED CA CERT: " + trustManagerKeyStore.getCertificate("ca"));
      jksKeyStoreInputStream.close();
      jksTrustStoreInputStream.close();

      // get private key
      Key privKey = keystore.getKey(keystoreAliasName, keyStorePassword.toCharArray());
      // Get certificate of public key
      X509Certificate cert = (X509Certificate) keystore.getCertificate(keystoreAliasName);



      TrustManager[] trustManagers;
      try {
        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustManagerKeyStore);
        trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
          throw new IllegalStateException(
              "Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        sslSocketFactory = sslContext.getSocketFactory();
      } catch (GeneralSecurityException e) {
        throw new RuntimeException(e);
      }

      LOG.info("\tCertificate Subject: " + cert.getSubjectDN());
      //GET 2.5.29.14	SubjectKeyIdentifier / 2.5.29.35	AuthorityKeyIdentifier
      String authorityKeyIndentifier = new String (cert.getExtensionValue("2.5.29.35"));
      String subjectKeyIdenfier = new String (cert.getExtensionValue("2.5.29.14"));
      LOG.info("AKI: " + authorityKeyIndentifier);
      LOG.info("SKI: " + subjectKeyIdenfier);
      //connectorUUID = subjectKeyIdenfier + ":" + authorityKeyIndentifier.substring(0, authorityKeyIndentifier.length() - 1);
      LOG.info("ConnectorUUID: " + connectorUUID);
      LOG.info("Retrieving Dynamic Attribute Token...");


      // create signed JWT (JWS)
      // Create expiry date one day (86400 seconds) from now
      Date expiryDate = Date.from(Instant.now().plusSeconds(86400));
      JwtBuilder jwtb =
          Jwts.builder()
              .setIssuer(connectorUUID)
              .setSubject(connectorUUID)
              .claim("@context", "https://w3id.org/idsa/contexts/context.jsonld")
              .claim("@type", "ids:DatRequestToken")
              .setExpiration(expiryDate)
              .setIssuedAt(Date.from(Instant.now()))
              .setAudience(targetAudience)
              .setNotBefore(Date.from(Instant.now()));

      String jws = jwtb.signWith(privKey, SignatureAlgorithm.RS256).compact();
      LOG.info("Request token: " + jws);

      // build form body to embed client assertion into post request
      RequestBody formBody =
          new FormBody.Builder()
              .add("grant_type", "client_credentials")
              .add(
                  "client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
              .add("client_assertion", jws)
              .add("scope", "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL")
              .build();

      OkHttpClient.Builder builder = new OkHttpClient.Builder();

      // configure client with trust manager
      OkHttpClient client =
          builder
              .sslSocketFactory(this.sslSocketFactory, (X509TrustManager) trustManagers[0])
              .connectTimeout(15, TimeUnit.SECONDS)
              .writeTimeout(15, TimeUnit.SECONDS)
              .readTimeout(15, TimeUnit.SECONDS)
              .build();

      Request request = new Request.Builder().url(dapsUrl + "/v2/token").post(formBody).build();
      Response jwtResponse = client.newCall(request).execute();
      if (!jwtResponse.isSuccessful()) {
        throw new IOException("Unexpected code " + jwtResponse);
      }
      var responseBody = jwtResponse.body();
      if (responseBody == null) {
        throw new Exception("JWT response is null.");
      }
      var jwtString = responseBody.string();
      LOG.info("Response body of token request:\n{}", jwtString);

      JSONObject jsonObject = new JSONObject(jwtString);
      dynamicAttributeToken = jsonObject.getString("access_token");

      LOG.info("Dynamic Attribute Token: " + dynamicAttributeToken);

      jwtClaims = verifyJWT(dynamicAttributeToken, dapsUrl);
    } catch (KeyStoreException
        | NoSuchAlgorithmException
        | CertificateException
        | UnrecoverableKeyException e) {
      LOG.error("Cannot acquire token:", e);
    } catch (IOException e) {
      LOG.error("Error retrieving token:", e);
    } catch (Exception e) {
      LOG.error("Something else went wrong:", e);
    }

    settings.setDynamicAttributeToken(dynamicAttributeToken);

    return jwtClaims;
  }

  @Override
  public Map<String, Object> verifyJWT(
      String dynamicAttributeToken,
      String dapsUrl) throws Exception {
    if (sslSocketFactory == null) {
      throw new JwtException("SSLSocketFactory is null, acquireToken() must be called first!");
    }

    try {
      // The HttpsJwks retrieves and caches keys from a the given HTTPS JWKS endpoint.
      // Because it retains the JWKs after fetching them, it can and should be reused
      // to improve efficiency by reducing the number of outbound calls the the endpoint.
      HttpsJwks httpsJkws = new HttpsJwks(dapsUrl + "/.well-known/jwks.json");
      Get getInstance = new Get();
      getInstance.setSslSocketFactory(sslSocketFactory);
      httpsJkws.setSimpleHttpGet(getInstance);

      // The HttpsJwksVerificationKeyResolver uses JWKs obtained from the HttpsJwks and will select
      // the most appropriate one to use for verification based on the Key ID and other factors
      // provided in the header of the JWS/JWT.
      HttpsJwksVerificationKeyResolver httpsJwksKeyResolver =
          new HttpsJwksVerificationKeyResolver(httpsJkws);

      // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
      // be used to validate and process the JWT.
      // The specific validation requirements for a JWT are context dependent, however,
      // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
      // and audience that identifies your system as the intended recipient.
      // If the JWT is encrypted too, you need only provide a decryption key or
      // decryption key resolver to the builder.
      JwtConsumer jwtConsumer =
          new JwtConsumerBuilder()
              .setRequireExpirationTime() // the JWT must have an expiration time
              .setAllowedClockSkewInSeconds(
                  30) // allow some leeway in validating time based claims to account for clock skew
              .setRequireSubject() // the JWT must have a subject claim
              .setExpectedIssuer(
                  "https://daps.aisec.fraunhofer.de") // whom the JWT needs to have been issued by
              //FIXME: Hardcoded two v1 and v2 values. Need to add versioning to correctly handle tokens.
              .setExpectedAudience(true, "IDS_Connector", "idsc:IDS_CONNECTORS_ALL") // to whom the JWT is intended for
              .setVerificationKeyResolver(httpsJwksKeyResolver)
              .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the
                  // given context
                  new org.jose4j.jwa.AlgorithmConstraints(
                      org.jose4j.jwa.AlgorithmConstraints.ConstraintType
                          .WHITELIST, // which is only RS256 here
                      AlgorithmIdentifiers.RSA_USING_SHA256))
              .build(); // create the JwtConsumer instance

      LOG.info("Verifying JWT...");
      //  Validate the JWT and process it to the Claims
      JwtClaims jwtClaims = jwtConsumer.processToClaims(dynamicAttributeToken);
      LOG.info("JWT validation succeeded! " + jwtClaims);

      return jwtClaims.getClaimsMap();
    } catch (InvalidJwtException e) {
      // InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
      // Hopefully with meaningful explanations(s) about what went wrong.
      LOG.warn("Invalid JWT!", e);

      // Programmatic access to (some) specific reasons for JWT invalidity is also possible
      // should you want different error handling behavior for certain conditions.

      // Whether or not the JWT has expired being one common reason for invalidity
      if (e.hasExpired()) {
        try {
          LOG.warn("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
        } catch (MalformedClaimException e1) {
          LOG.error("Malformed claim encountered", e1);
        }
      }

      // Or maybe the audience was invalid
      if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID)) {
        try {
          LOG.warn("JWT had wrong audience: " + e.getJwtContext().getJwtClaims().getAudience());
        } catch (MalformedClaimException e1) {
          LOG.error("Malformed claim encountered", e1);
        }
      }

      throw e;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void validateDATSecurityAttributes(Map<String, Object> claims, ConnectionSettings connectionSettings)
      throws DatException {
    try {

      String securityProfile = claims.get("securityProfile").toString();

      //FIXME: Validate security profile the proper way

      ArrayList<String> validTrustProfiles=new ArrayList<String>();
      validTrustProfiles.add("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE");
      validTrustProfiles.add("idsc:BASE_CONNECTOR_SECURITY_PROFILE");
      validTrustProfiles.add("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE");

      if (!validTrustProfiles.contains(securityProfile)) {
        throw new DatException("Client does not support valid trust profile.");
      }
      //TODO Check for trust profile

      if(connectionSettings.getRequiredSecurityProfile().equals("idsc:BASE_CONNECTOR_SECURITY_PROFILE")) {
        if (!securityProfile.equals("idsc:BASE_CONNECTOR_SECURITY_PROFILE")
            && !securityProfile.equals("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE")
            && !securityProfile.equals("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")) {

          throw new DatException(
              "Client does not support required trust profile: Required: "
                  + connectionSettings.getRequiredSecurityProfile()
                  + " given: "
                  + securityProfile);
        }
      } else if(connectionSettings.getRequiredSecurityProfile().equals("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE")) {
        if (!securityProfile.equals("idsc:TRUSTED_CONNECTOR_SECURITY_PROFILE") &&
            !securityProfile.equals("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")) {
          throw new DatException(
              "Client does not support required trust profile: Required: "
                  + connectionSettings.getRequiredSecurityProfile()
                  + " given: "
                  + securityProfile);
        }
      } else if(connectionSettings.getRequiredSecurityProfile().equals("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")) {
          if (!securityProfile.equals("idsc:TRUSTED_CONNECTOR_PLUS_SECURITY_PROFILE")) {
            throw new DatException(
                "Client does not support required trust profile: Required: "
                    + connectionSettings.getRequiredSecurityProfile()
                    + " given: "
                    + securityProfile);
          }
      } else {
        throw new DatException(
            "Client does not support any valid trust profile: Required: "
                + connectionSettings.getRequiredSecurityProfile()
                + " given: "
                + securityProfile);
      }

      // TODO: validate further security attributes
    } catch (NumberFormatException e) {
      throw new DatException("Connection settings contains an invalid number format.", e);
    }
  }

  @Activate
  public void run() {
    LOG.info("Token renewal triggered.");
    try {
      ConnectorConfig config = settings.getConnectorConfig();
      acquireToken(
          FileSystems.getDefault().getPath("etc"),
          config.getDapsUrl(),
          config.getKeystoreName(),
          config.getKeystorePassword(),
          config.getKeystoreAliasName(),
          config.getTruststoreName(),
          config.getConnectorUUID());

    } catch (Exception e) {
      LOG.error("Token renewal failed", e);
    }
  }
}
