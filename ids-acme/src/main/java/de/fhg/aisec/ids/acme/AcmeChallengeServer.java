/*-
 * ========================LICENSE_START=================================
 * ACME v2 client
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
package de.fhg.aisec.ids.acme;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcmeChallengeServer {
    public static final String TEXT_PLAIN = "text/plain";
    public static final Pattern ACME_REGEX = Pattern.compile("^.*?/\\.well-known/acme-challenge/(.+)$");
    private static NanoHTTPD server = null;
    private static final Logger LOG = LoggerFactory.getLogger(AcmeChallengeServer.class);

    private AcmeChallengeServer() { /* hides public c'tor */ }
    
    public static void startServer(final AcmeClient acmeClient) throws IOException {
        server = new NanoHTTPD(5002) {
            @Override
            public Response serve(IHTTPSession session) {
                Matcher tokenMatcher = ACME_REGEX.matcher(session.getUri());
                if (!tokenMatcher.matches()) {
                    LOG.error("Received invalid ACME challenge {} ", session.getUri());
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, TEXT_PLAIN, null);
                }
                String token = tokenMatcher.group(1);
                LOG.info("Received ACME challenge: {}", token);
                String response = acmeClient.getChallengeAuthorization(token);
                if (response == null) {
                    LOG.warn("ACME challenge is unknown");
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, TEXT_PLAIN, null);
                } else {
                    LOG.info("ACME challenge response: {}", response);
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, TEXT_PLAIN,
                            new ByteArrayInputStream(responseBytes), responseBytes.length);
                }
            }
        };
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
        LOG.debug("NanoHTTPD started");
    }

    public static void stopServer() {
        server.stop();
        LOG.debug("NanoHTTPD stopped");
    }
}
