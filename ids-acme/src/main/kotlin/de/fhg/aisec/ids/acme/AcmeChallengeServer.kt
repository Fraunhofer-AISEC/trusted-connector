/*-
 * ========================LICENSE_START=================================
 * ids-acme
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
package de.fhg.aisec.ids.acme

import de.fhg.aisec.ids.api.acme.AcmeClient
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object AcmeChallengeServer {
    const val TEXT_PLAIN = "text/plain"
    val ACME_REGEX = Pattern.compile("^.*?/\\.well-known/acme-challenge/(.+)$")!!
    private var server: NanoHTTPD? = null
    private val LOG = LoggerFactory.getLogger(AcmeChallengeServer::class.java)!!

    @Throws(IOException::class)
    fun startServer(acmeClient: AcmeClient, challengePort: Int) {
        server =
            object : NanoHTTPD(challengePort) {
                override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
                    val tokenMatcher = ACME_REGEX.matcher(session.uri)
                    if (!tokenMatcher.matches()) {
                        LOG.error("Received invalid ACME challenge {} ", session.uri)
                        return NanoHTTPD.newFixedLengthResponse(
                            NanoHTTPD.Response.Status.BAD_REQUEST,
                            TEXT_PLAIN,
                            null
                        )
                    }
                    val token = tokenMatcher.group(1)
                    LOG.info("Received ACME challenge: {}", token)
                    val response = acmeClient.getChallengeAuthorization(token)
                    return if (response == null) {
                        LOG.warn("ACME challenge is unknown")
                        NanoHTTPD.newFixedLengthResponse(
                            NanoHTTPD.Response.Status.NOT_FOUND,
                            TEXT_PLAIN,
                            null
                        )
                    } else {
                        LOG.info("ACME challenge response: {}", response)
                        val responseBytes = response.toByteArray(StandardCharsets.UTF_8)
                        NanoHTTPD.newFixedLengthResponse(
                            NanoHTTPD.Response.Status.OK,
                            TEXT_PLAIN,
                            ByteArrayInputStream(responseBytes),
                            responseBytes.size.toLong()
                        )
                    }
                }
            }
        server?.let {
            it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true)
            LOG.debug("NanoHTTPD started")
        }
    }

    fun stopServer() {
        server?.let {
            it.stop()
            server = null
            LOG.debug("NanoHTTPD stopped")
        }
    }
}
