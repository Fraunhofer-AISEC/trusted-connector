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
package de.fhg.aisec.ids.webconsole.api;

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API interface for Connector settings in the connector.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
 */

// ConnectorProfile will be processed by custom Jackson deserializer
@Path("/settings")
@Api(
  value = "Self-Description and Connector Profiles",
  authorizations = {@Authorization(value = "oauth2")}
)
public class SettingsApi {
  private static final Logger LOG = LoggerFactory.getLogger(SettingsApi.class);

  @POST
  @Path("/connectorProfile")
  @ApiOperation(value = "Configure the connector's self-description (\"Connector Profile\").")
  @Consumes(MediaType.APPLICATION_JSON)
  @AuthorizationRequired
  public String postConnectorProfile(ConnectorProfile profile) {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    if (im.setConnector(profile)) {
      return "ConnectorProfile successfully stored.";
    } else {
      throw new InternalServerErrorException("Error while storing ConnectorProfile");
    }
  }

  /** Returns Connector profile based on currently stored preferences or empty Connector profile */
  @GET
  @Path("/connectorProfile")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
    value = "Returns this connector's self-description (\"Connector Profile\")",
    response = ConnectorProfile.class
  )
  @AuthorizationRequired
  public ConnectorProfile getConnectorProfile() {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    Connector c = im.getConnector();
    if (c == null) {
      return new ConnectorProfile();
    } else {
      return new ConnectorProfile(
          c.getSecurityProfile(),
          c.getId(),
          c.getMaintainer(),
          c.getDescription().stream().map(PlainLiteral.class::cast).collect(Collectors.toList()));
    }
  }

  /**
   * Returns connector profile based on currently stored preferences or statically provided JSON-LD
   * model, or empty connector profile if none of those are available.
   */
  @GET
  @Path("/selfInformation")
  @Produces("application/ld+json")
  // TODO Document ApiOperation
  @AuthorizationRequired
  public String getSelfInformation() {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    try {
      return im.getConnectorAsJsonLd();
    } catch (NullPointerException e) {
      LOG.warn("Connector description build failed, building empty description.", e);
      return null;
    }
  }

  /** Set static connector profile based on passed JSON-LD data */
  @POST
  @Path("/selfInformation")
  // TODO Document ApiOperation
  @Consumes("application/ld+json")
  @AuthorizationRequired
  public void setSelfInformation(String selfInformation) {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    try {
      im.setConnectorByJsonLd(selfInformation);
    } catch (NullPointerException e) {
      LOG.warn("Connector description build failed, building empty description.", e);
    }
  }

  /** Remove static connector profile based on JSON-LD data */
  @DELETE
  @Path("/selfInformation")
  // TODO Document ApiOperation
  @Consumes("application/ld+json")
  @AuthorizationRequired
  public void removeSelfInformation() {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    try {
      im.setConnectorByJsonLd(null);
    } catch (NullPointerException e) {
      LOG.warn("Connector description build failed, building empty description.", e);
    }
  }
}
