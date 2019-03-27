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

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.stream.Collectors;


/**
 * REST API interface for Connector settings in the connector.
 * <p>
 * The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
 */

//ConnectorProfile will be processed by custom Jackson deserializer
@Path("/settings")
public class SettingsApi {
  private static final Logger LOG = LoggerFactory.getLogger(SettingsApi.class);

  @POST
  @Path("/connectorProfile")
  @Consumes(MediaType.APPLICATION_JSON)
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

  /**
   * Returns Connector profile based on currently stored preferences or empty Connector profile
   */
  @GET
  @Path("/connectorProfile")
  @Produces(MediaType.APPLICATION_JSON)
  public ConnectorProfile getConnectorProfile() {
    InfoModel im = WebConsoleComponent.getInfoModelManager();
    if (im == null) {
      throw new ServiceUnavailableException("InfoModel is not available");
    }
    try {
      Connector c = im.getConnector();
      return new ConnectorProfile(
              c.getSecurityProfile(),
              c.getId(),
              c.getMaintainer(),
              c.getDescriptions().stream().map(PlainLiteral.class::cast).collect(Collectors.toList()));
    } catch (NullPointerException e) {
      LOG.warn("Connector description build failed, building empty description.", e);
      return new ConnectorProfile();
    }
  }

  /**
   * Returns Connector profile based on currently stored preferences or empty Connector profile
   */
  @GET
  @Path("/selfInformation")
  @Produces("application/ld+json")
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

}
