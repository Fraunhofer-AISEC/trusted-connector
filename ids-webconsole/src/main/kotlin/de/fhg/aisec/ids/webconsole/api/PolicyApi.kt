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

import de.fhg.aisec.ids.api.policy.PAP
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.Consumes
import javax.ws.rs.DefaultValue
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * REST API interface for managing usage control policies in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/policies/<method>.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
</method> */
@Component
@Path("/policies")
@Api(value = "Usage Control Policies", authorizations = [Authorization(value = "oauth2")])
class PolicyApi {

    @Autowired(required = false)
    private var policyAdministrationPoint: PAP? = null

    @GET
    @Path("list")
    @ApiOperation(value = "Lists active usage control rules", responseContainer = "List")
    @ApiResponses(
        ApiResponse(
            code = 200,
            message = "List of usage control rules",
            response = String::class,
            responseContainer = "List"
        )
    )
    @Produces(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun list() = policyAdministrationPoint?.listRules() ?: emptyList()

    /**
     * Returns the Prolog theory of all policies. Could be removed in later version.
     *
     * @return Policy Prolog
     */
    @get:AuthorizationRequired
    @get:ApiOperation(value = "Returns the full usage control policy as a Prolog theory")
    @get:Produces(MediaType.TEXT_PLAIN)
    @get:Path("policyProlog")
    @get:GET
    val policyProlog: String
        get() = policyAdministrationPoint?.policy ?: "No PAP available"

    @POST
    @Path("install")
    @ApiOperation(value = "Installs a new usage control policy as a Prolog theory file")
    @Consumes(
        MediaType.MULTIPART_FORM_DATA
    )
    @AuthorizationRequired
    fun install(
        @FormParam(value = "policy_name") @DefaultValue(value = "default policy") policyName: String?,
        @FormParam(value = "policy_description") @DefaultValue(value = "") policyDescription: String?,
        @FormParam(value = "policy_file") policy: String
    ): String {
        LOG.info("Received policy file. name: {}, desc: {}", policyName, policyDescription)
        return policyAdministrationPoint?.let { pap ->
            pap.loadPolicy(policy)
            "OK"
        } ?: "No PAP available"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PolicyApi::class.java)
    }
}
