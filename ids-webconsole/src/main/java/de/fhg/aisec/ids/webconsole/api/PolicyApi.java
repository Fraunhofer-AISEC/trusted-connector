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

import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API interface for managing "apps" in the connector.
 *
 * <p>In this implementation, apps are either docker or trustX containers.
 *
 * <p>The API will be available at http://localhost:8181/cxf/api/v1/policies/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Path("/policies")
@Api("Policies")
public class PolicyApi {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyApi.class);

  @GET
  @Path("list")
  @ApiOperation(value = "Lists active rules", responseContainer = "List")
  @ApiResponses(
      @ApiResponse(
        code = 200,
        message = "List of usage control rules",
        response = String.class,
        responseContainer = "List"
      ))
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> list() {
    // TODO JS->ML: Hier sollte vlt. eher eine Liste von Policy-Objekten zurückgegeben werden.
    return WebConsoleComponent.getPolicyAdministrationPointOrThrowSUE().listRules();
  }

  /**
   * Returns the Prolog theory of all policies. Could be removed in later version.
   *
   * @return Policy Prolog
   */
  @GET
  @Path("policyProlog")
  @Produces(MediaType.TEXT_PLAIN)
  public String getPolicyProlog() {
    return WebConsoleComponent.getPolicyAdministrationPointOrThrowSUE().getPolicy();
  }

  @POST
  @OPTIONS
  @Path("install")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public String install(
      @Multipart(value = "policy_name") @DefaultValue(value = "default policy") String policyName,
      @Multipart(value = "policy_description") @DefaultValue(value = "") String policyDescription,
      @Multipart(value = "policy_file") InputStream is) {
    LOG.info("Received policy file. name: {}, desc: {}", policyName, policyDescription);
    WebConsoleComponent.getPolicyAdministrationPointOrThrowSUE().loadPolicy(is);
    return "OK";
  }

  // TODO JS->ML: Endpoints für policy modification, ggf. weitere

}
