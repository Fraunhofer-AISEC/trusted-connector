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

import java.util.Collection;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fraunhofer.iais.eis.ConnectorBuilder;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.util.PlainLiteral;


/**
 * REST API interface for settings in the connector.
 * <p>
 * The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
 */


@Path("/settings")
public class SettingsApi {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsApi.class);

    @POST
    @Path("/connectorProfile")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postConnectorProfile(ConnectorProfile cP) {

        InfoModel im = WebConsoleComponent.getInfoModelManagerOrThrowSUE();
        if (im.setConnector(cP.getConnectorURL(), cP.getOperatorURL(), cP.getConnectorEntityNames(), cP.getSecurityProfile()))
            return Response.status(200).entity("Connector object successfully stored.").build();
        else
            return Response.status(500).entity("Connector object couldn't be stored.").build();
    }

    @GET
    @Path("/connectorProfile")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorProfile getConnectorProfile() {

        InfoModel im = WebConsoleComponent.getInfoModelManagerOrThrowSUE();
        Connector c = im.getConnector();
        if (c != null) {
            return new ConnectorProfile(
                    c.getSecurityProfile(),
                    c.getId(),
                    c.getOperator(),
                    (Collection<PlainLiteral>) c.getEntityNames());
        } else {
            LOG.debug("Building empty connector description.");
            return new ConnectorProfile();
        }
    }

}
