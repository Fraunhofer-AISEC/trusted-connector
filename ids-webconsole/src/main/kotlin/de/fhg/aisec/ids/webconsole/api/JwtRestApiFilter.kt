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
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * This filter verifies JWT bearer tokens provided by HTTP "Authorization" header.
 *
 * @author Michael Lux
 */
@Component
class JwtRestApiFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        try {
            // Get JWT Bearer token from HTTP Authorization header
            val authorizationHeader = request.getHeader("Authorization") ?: run {
                response.reset()
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.writer.write("Authorization token missing.")
                return
            }
            // Verify token
            VERIFIER.verify(authorizationHeader.substring(7).trim())
        } catch (e: Exception) {
            LOG.warn("Invalid JWT token in request, sending 401 UNAUTHORIZED...")
            response.reset()
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("Invalid token or auth error.")
            return
        }
        // Continue filter chain
        chain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI.let {
            it == "/api/v1/user/login" || !it.startsWith("/api/v1")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(JwtRestApiFilter::class.java)
        private val VERIFIER = JWT.require(Algorithm.HMAC256(UserApi.key)).withIssuer("ids-connector").build()
    }
}
