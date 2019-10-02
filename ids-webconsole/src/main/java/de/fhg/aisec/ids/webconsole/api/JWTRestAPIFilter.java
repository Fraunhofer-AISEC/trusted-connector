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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * This filter verifies JWT bearer tokens provided by HTTP "Authorization" header.
 * <p>
 *
 * @author Hendrik Meyer zum Felde
 * hendrik.meyerzumfelde@aisec.fraunhofer.de
 */

@Provider
@Component(immediate = true)
@AuthorizationRequired
public class JWTRestAPIFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JWTRestAPIFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            //Get JWT Bearer token from HTTP Authorization header
            String authorizationHeader = requestContext.getHeaderString("Authorization");
            if (authorizationHeader == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Authorization token missing.").build());
                return;
            }

            // Verify token
            String jwt = authorizationHeader.substring(7).trim();
            Algorithm algorithm = Algorithm.HMAC256(UserApi.key.getEncoded());
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("ids-connector")
                    .build(); //Reusable verifier instance
            DecodedJWT validToken = verifier.verify(jwt);
        } catch (Exception e) {
            LOG.debug("Invalid JWT token in request for " + requestContext.getUriInfo().getPath());
            // On token validation error (or other), return HTTP 403.
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token.").build());
        }
    }
}