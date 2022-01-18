/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
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
package de.fhg.aisec.ids.webconsole.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

/**
 * This filter verifies JWT bearer tokens provided by HTTP "Authorization" header.
 *
 *
 *
 *
 * @author Hendrik Meyer zum Felde hendrik.meyerzumfelde@aisec.fraunhofer.de
 */
@Provider
@AuthorizationRequired
class JWTRestAPIFilter : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext) {
        try {
            // Get JWT Bearer token from HTTP Authorization header
            val authorizationHeader = requestContext.getHeaderString("Authorization")
            if (authorizationHeader == null) {
                requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Authorization token missing.")
                        .build()
                )
                return
            }

            // Verify token
            val jwt = authorizationHeader.substring(7).trim { it <= ' ' }
            val algorithm: Algorithm = Algorithm.HMAC256(UserApi.key)
            val verifier = JWT.require(algorithm).withIssuer("ids-connector").build() // Reusable verifier instance
            verifier.verify(jwt)
        } catch (e: Exception) {
            LOG.warn("Invalid JWT token in request for " + requestContext.uriInfo.path)
            // On token validation error (or other), return HTTP 403.
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token.").build()
            )
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(JWTRestAPIFilter::class.java)
    }
}
