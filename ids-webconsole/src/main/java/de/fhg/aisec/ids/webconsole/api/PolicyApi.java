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

import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import io.swagger.annotations.*;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * REST API interface for managing usage control policies in the connector.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/policies/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Component
@Path("/policies")
@Api(
  value = "Usage Control Policies",
  authorizations = {@Authorization(value = "oauth2")}
)
public class PolicyApi {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyApi.class);

  @GET
  @Path("list")
  @ApiOperation(value = "Lists active usage control rules", responseContainer = "List")
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message = "List of usage control rules",
        response = String.class,
        responseContainer = "List"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  @AuthorizationRequired
  public List<String> list() {
    PAP pap = WebConsoleComponent.getPolicyAdministrationPoint();
    if (pap == null) {
      return new ArrayList<>();
    }
    return pap.listRules();
  }

  /**
   * Returns the Prolog theory of all policies. Could be removed in later version.
   *
   * @return Policy Prolog
   */
  @GET
  @Path("policyProlog")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the full usage control policy as a Prolog theory")
  @AuthorizationRequired
  public String getPolicyProlog() {
    PAP pap = WebConsoleComponent.getPolicyAdministrationPoint();
    if (pap == null) {
      return "";
    }
    return pap.getPolicy();
  }

  @POST
  //@OPTIONS
  @Path("install")
  @ApiOperation(value = "Installs a new usage control policy as a Prolog theory file")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @AuthorizationRequired
  public String install(
      @FormParam(value = "policy_name") @DefaultValue(value = "default policy") String policyName,
      @FormParam(value = "policy_description") @DefaultValue(value = "") String policyDescription,
      @FormParam(value = "policy_file") String policy) {
    LOG.info("Received policy file. name: {}, desc: {}", policyName, policyDescription);
    PAP pap = WebConsoleComponent.getPolicyAdministrationPoint();
    if (pap == null) {
      return "No PAP available";
    }
    pap.loadPolicy(policy);
    return "OK";
  }
}
