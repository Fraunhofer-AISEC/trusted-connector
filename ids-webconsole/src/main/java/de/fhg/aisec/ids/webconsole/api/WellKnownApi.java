/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole.api;

import de.fhg.aisec.ids.api.acme.AcmeClient;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;

@Path("/.well-known")
public class WellKnownApi {
    private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);

    @GET
    @Path("/acme-challenge/{challenge}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAcmeChallenge(@PathParam("challenge") String challenge) {
        AcmeClient client = WebConsoleComponent.getAcmeClient();
        String authorization = client.getChallengeAuthorization(challenge);
        LOG.info("Answering ACME challenge " + challenge + " with " + authorization);
        return authorization;
    }
}
