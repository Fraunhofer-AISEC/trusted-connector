/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.osgi.service.component.annotations.Component;

/**
 * This filter performs JWT checks on a given map of API endpoints and corresponding required access roles.
 *
 * Features:  Validation of JWTs using stored or retrieved keys
 *            Retrieving JWKS from an issuer's endpoint (de/activate via commenting)
 *
 * @author Hendrik Meyer zum Felde
 * hendrik.meyerzumfelde@aisec.fraunhofer.de
 */

@Provider
@Component(immediate = true)
public class JWTRestAPIFilter implements ContainerRequestFilter {
  //TDBL build hashmap outside of function
  private Boolean filterForJWTisActive = false; //switch to "true" to activate filter
  private HashMap<String, String> endpointsMappingAccessRoles = new HashMap<String, String>();
  private JwtConsumer jwtConsumer;
  private JwtConsumerBuilder jwtConsumerBuilder;
  private JwtConsumer firstPassJwtConsumer;
  private String JWT_FILTER_STRING_TO_IDENTIFY_ROLES = "resource_access.connector.roles";
  private String JWT_ISSUER = "http://testissuer.test";
  private HttpsJwks httpsJkws = null;

  public JWTRestAPIFilter() {
    //Hardcoded mapping of ENDPOINT and ACCESS_ROLE. If an endpoint is listed here, it requires JWT with role
    //other endpoints, which are commented out, do not require a JWT authorization. -> Blacklisting TBDL Whitelisting

/*  endpointsMappingAccessRoles.put("GET-app/list", "ROLE_APP_GET_LIST");
    endpointsMappingAccessRoles.put("GET-app/start/{Id}", "ROLE_APP_START");
    // Endpoint has a double dynamic endpath, its handled in main Code below
    endpointsMappingAccessRoles.put("GET-app/start/{Id}/{key}", "ROLE_APP_START_WITH_KEY");
    endpointsMappingAccessRoles.put("GET-app/stop/{Id}", "ROLE_APP_STOP");
    endpointsMappingAccessRoles.put("POST-app/install", "ROLE_APP_INSTALL");
    endpointsMappingAccessRoles.put("GET-app/wipe", "ROLE_APP_WIPE");
    endpointsMappingAccessRoles.put("GET-app/cml_version", "ROLE_APP_CML_VERSION");
    endpointsMappingAccessRoles.put("POST-app/search", "ROLE_APP_SEARCH");
    endpointsMappingAccessRoles.put("GET-certs/acme_renew/{Id}", "ROLE_CERTS_ACME_RENEW");
    endpointsMappingAccessRoles.put("GET-certs/acme_tos", "ROLE_CERTS_GET_TOS");
    endpointsMappingAccessRoles.put("GET-certs/list_certs", "ROLE_CERTS_GET_CERTS");
    endpointsMappingAccessRoles.put("GET-certs/list_identities", "ROLE_CERTS_GET_IDENTITIES");
    endpointsMappingAccessRoles.put("POST-certs/create_identity", "ROLE_CERTS_CREATE_IDENTITY");
    endpointsMappingAccessRoles.put("POST-certs/delete_identity", "ROLE_CERTS_DELETE_IDENTITY");
    endpointsMappingAccessRoles.put("POST-certs/delete_cert", "ROLE_CERTS_DELETE_CERTIFICATE");
    endpointsMappingAccessRoles.put("POST-certs/install_trusted_cert", "ROLE_CERTS_INSTALL_TRUSTED_CERT");
    endpointsMappingAccessRoles.put("GET-config", "ROLE_CONFIG_GET_GENERAL_CONFIG");
    endpointsMappingAccessRoles.put("POST-config", "ROLE_CONFIG_SET_GENERAL_CONFIG");
    endpointsMappingAccessRoles.put("POST-config/connectionConfigs/{Id}", "ROLE_CONFIG_SET_CONNECTION_CONFIG");
    endpointsMappingAccessRoles.put("GET-config/connectionConfigs/{Id}", "ROLE_CONFIG_GET_CONNECTION_CONFIG");
    endpointsMappingAccessRoles.put("GET-config/connectionConfigs", "ROLE_CONFIG_GET_CONNECTIONS_CONFIGS");
    endpointsMappingAccessRoles.put("GET-connections/incoming", "ROLE_CONNECTIONS_INCOMING");
    endpointsMappingAccessRoles.put("GET-connections/outgoing", "ROLE_CONNECTIONS_OUTGOING");
    endpointsMappingAccessRoles.put("GET-connections/endpoints", "ROLE_CONNECTIONS_ENDPOINTS");
*/  endpointsMappingAccessRoles.put("POST-licenses/upload_license", "ROLE_LICENSES_UPLOAD");
    //deactivated due to public purpose
    //endpointsMappingAccessRoles.put("GET-licenses/get_license/{Id}", "ROLE_LICENSES_DOWNLOAD");
/*  endpointsMappingAccessRoles.put("GET-metric/get", "ROLE_METRIC_GET");
    endpointsMappingAccessRoles.put("GET-policies/list", "ROLE_POLICIES_GET_LIST");
    endpointsMappingAccessRoles.put("GET-policies/policyProlog", "ROLE_POLICIES_GET_PROLOG");
    endpointsMappingAccessRoles.put("POST-policies/install", "ROLE_POLICIES_POST_POLICIES");
    endpointsMappingAccessRoles.put("GET-routes/list", "ROLE_ROUTES_GET_LIST");
    endpointsMappingAccessRoles.put("GET-routes/get/{Id}", "ROLE_ROUTES_GET_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("GET-routes/getAsString/{Id}", "ROLE_ROUTES_GET_SINGLE_ROUTE_AS_STRING");
    endpointsMappingAccessRoles.put("GET-routes/startroute/{Id}", "ROLE_ROUTES_START_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("POST-routes/save/{Id}", "ROLE_ROUTES_SAVE_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("PUT-routes/add", "ROLE_ROUTES_ADDS_NEW_ROUTE");
    endpointsMappingAccessRoles.put("GET-routes/stoproute/{Id}", "ROLE_ROUTES_STOP_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("GET-routes/metrics/{Id}", "ROLE_ROUTES_GET_METRIC_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("GET-routes/metrics", "ROLE_ROUTES_GET_METRIC_ALL_ROUTES");
    endpointsMappingAccessRoles.put("GET-routes/list_endpoints", "ROLE_ROUTES_LIST_ENDPOINTS");
    endpointsMappingAccessRoles.put("GET-routes/components", "ROLE_ROUTES_LIST_COMPONENTS");
    endpointsMappingAccessRoles.put("GET-routes/validate/{Id}", "ROLE_ROUTES_VALIDATE_SINGLE_ROUTE");
    endpointsMappingAccessRoles.put("GET-routes/prolog/{Id}", "ROLE_ROUTES_GET_PROLOG_SINGLE_ROUTE");
*/  endpointsMappingAccessRoles.put("POST-settings/connectorProfile", "ROLE_SETTINGS_SET_CONNECTOR_PROFILE");
    endpointsMappingAccessRoles.put("GET-settings/connectorProfile", "ROLE_SETTINGS_GET_CONNECTOR_PROFILE");
    endpointsMappingAccessRoles.put("POST-settings/selfInformation", "ROLE_SETTINGS_SELF_INFORMATION_SAVE");
    //deactivated due to public purpose
    //endpointsMappingAccessRoles.put("GET-settings/selfInformation", "ROLE_SETTINGS_SELF_INFORMATION_GET");
    endpointsMappingAccessRoles.put("DELETE-settings/selfInformation", "ROLE_SETTINGS_SELF_INFORMATION_DELETE");



    //Setting the JWT consumer
    jwtConsumerBuilder = new JwtConsumerBuilder()
            .setSkipDefaultAudienceValidation()
            .setRequireExpirationTime() // the JWT must have an expiration time
            .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
            .setExpectedIssuer(JWT_ISSUER) // whom the JWT needs to have been issued by
            .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                    new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, // which is only RS256 here
                            AlgorithmIdentifiers.RSA_USING_SHA256, AlgorithmIdentifiers.RSA_USING_SHA256))
            ; // create the JwtConsumer instance
    //Code for Key Retrieval DEBUGGING USAGE
/*    String filePathPublicKey = "C:\\public_key.der";
    PublicKey myPublicKey = null;
    try {
      myPublicKey = getPublicKey(filePathPublicKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    jwtConsumerBuilder.setVerificationKey(myPublicKey) // verify the signature with the public key
    jwtConsumer = jwtConsumerBuilder.build();
*/

    //Setting the firstPassJwtConsumer (used to crack open the JWT and read the issuer to get the JWKS from an endpoint)
    // it doesn't check signatures or does any validation.
    JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
            .setSkipAllValidators()
            .setDisableRequireSignature()
            .setSkipSignatureVerification()
            .build();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    if (!filterForJWTisActive) return;
    //Getting JWT Bearer token from HTTP Authorization header
    String authorizationHeader = null;
    String jwt = null;
    try {//Handle case where no Authentication Token is present
      authorizationHeader = requestContext.getHeaderString("Authorization");
      jwt = authorizationHeader.substring("Bearer".length()).trim();
    }
      catch (Exception e)
    {
    }

    //JWKS: getting the public key from provided endpoint service / alternative to static public key setting above
    try {
      JwtContext firstPassJwtContext = firstPassJwtConsumer.process(jwt);
      String issuer = firstPassJwtContext.getJwtClaims().getIssuer();
      HttpsJwks httpsJkws = new HttpsJwks(issuer);
      HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
      jwtConsumerBuilder.setVerificationKeyResolver(httpsJwksKeyResolver);
      jwtConsumer = jwtConsumerBuilder.build();
    } catch (InvalidJwtException e) {
      e.printStackTrace();
    } catch (MalformedClaimException e) {
      e.printStackTrace();
    }

    //Getting HTTP Request path
    UriInfo uriInfo = requestContext.getUriInfo();
    String requestVerbAndPath = requestContext.getMethod() + "-" + uriInfo.getPath(); //TODO check if ASCII or string ok
    // Delete last "/" if present
    if (requestVerbAndPath.substring(requestVerbAndPath.length() - 1).equals("/"))
      requestVerbAndPath = requestVerbAndPath.substring(0, requestVerbAndPath.length() - 1);

 /*   //Match dynamic endpoints .../start/123413 to .../start/{id} and start/123421/kevalue123 to start/{Id}/{key}. TDBL
    if (!endpointsMappingAccessRoles.containsKey(requestVerbAndPath)) {
      String requestVerbAndPathLastPathCut = requestVerbAndPath.substring(0, requestVerbAndPath.lastIndexOf("/"));
      if (!requestVerbAndPath.contains("GET-app/start")) {
        requestVerbAndPath = requestVerbAndPathLastPathCut + "/{Id}";
      } else {
        if (requestVerbAndPathLastPathCut.equals("GET-app/start")) {
          requestVerbAndPath = requestVerbAndPathLastPathCut += "/{Id}";
        } else {
          String requestVerbAndPathLastPathCutTwice = requestVerbAndPathLastPathCut.substring(0,
                  requestVerbAndPathLastPathCut.lastIndexOf("/"));
          if (requestVerbAndPathLastPathCutTwice.equals("GET-app/start")) {
            requestVerbAndPath = requestVerbAndPathLastPathCutTwice += "/{Id}/{key}";
          }
        }
      }
    }
*/

    //Checking if the path is protected and proceed normally if API endpoint is not in protected endpointMappingList
    if (!endpointsMappingAccessRoles.containsKey(requestVerbAndPath)) return;
    String requiredRole = endpointsMappingAccessRoles.get(requestVerbAndPath);
    try {
      if (!jwt.isEmpty()) {
        //Validate the JWT and check if JWT contains access to roles
        JwtClaims jwtClaims = null;
        try {
          jwtClaims = jwtConsumer.processToClaims(jwt);
        } catch (InvalidJwtException e) {
          requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                  .entity("ERROR: Could not process claims from JWT.").build());
        }
        try {//transform json to flattened map for easier access to roles
          Map<String, List<Object>> flattenedJwtClaims = jwtClaims.flattenClaims();
          List<Object> connectorRolesList = flattenedJwtClaims.get(JWT_FILTER_STRING_TO_IDENTIFY_ROLES);
          Boolean privilegesGiven = connectorRolesList.contains(requiredRole);
          if (privilegesGiven) {
            return; //access granted
          } else {//Required access roles missing.
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("ERROR: JWT can be read but does not provide the required access rights.").build());
          }
        } catch (Exception e) {//Could not flatten the JWT according to the configured structure
          requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                  .entity("ERROR: Specified JWT role structure is not fitting the JWT.").build());
        }
      }
    }
    catch (Exception e) {//JWT is empty -> no auth
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
              .entity("ERROR: No JWT Token provided for authorization.").build());
    }
  }

  private PublicKey getPublicKey(String filename) throws Exception {
    byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
    X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePublic(spec);
  }
}