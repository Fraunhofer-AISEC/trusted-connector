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

import de.fhg.aisec.ids.api.infomodel.ConnectorProfile
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fraunhofer.iais.eis.util.TypedLiteral
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.stream.Collectors
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.InternalServerErrorException
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * REST API interface for Connector settings in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
</method> */
// ConnectorProfile will be processed by custom Jackson deserializer
@Component
@Path("/settings")
@Api(value = "Self-Description and Connector Profiles", authorizations = [Authorization(value = "oauth2")])
class SettingsApi {

    @Autowired
    private lateinit var im: InfoModel

    @POST
    @Path("/connectorProfile")
    @ApiOperation(value = "Configure the connector's self-description (\"Connector Profile\").")
    @Consumes(
        MediaType.APPLICATION_JSON
    )
    @AuthorizationRequired
    fun postConnectorProfile(profile: ConnectorProfile?): String {
        return if (im.setConnector(profile!!)) {
            "ConnectorProfile successfully stored."
        } else {
            throw InternalServerErrorException("Error while storing ConnectorProfile")
        }
    }

    /** Returns Connector profile based on currently stored preferences or empty Connector profile  */
    @get:AuthorizationRequired
    @get:ApiOperation(
        value = "Returns this connector's self-description (\"Connector Profile\")",
        response = ConnectorProfile::class
    )
    @get:Produces(MediaType.APPLICATION_JSON)
    @get:Path("/connectorProfile")
    @get:GET
    val connectorProfile: ConnectorProfile
        get() {
            val c = im.connector
            return if (c == null) {
                ConnectorProfile()
            } else {
                ConnectorProfile(
                    c.securityProfile,
                    c.id,
                    c.maintainer,
                    c.description.stream().map { obj: Any? -> TypedLiteral::class.java.cast(obj) }
                        .collect(Collectors.toList())
                )
            }
        }

    /**
     * Returns connector profile based on currently stored preferences or statically provided JSON-LD
     * model, or empty connector profile if none of those are available.
     */
    @get:Produces("application/ld+json")
    @get:Path("/selfInformation")
    @get:GET
    @set:AuthorizationRequired
    @set:Consumes("application/ld+json")
    @set:Path("/selfInformation")
    @set:POST
    var selfInformation: String?
        // TODO Document ApiOperation
        get() = try {
            im.connectorAsJsonLd
        } catch (e: NullPointerException) {
            LOG.warn("Connector description build failed, building empty description.", e)
            null
        }
        // TODO Document ApiOperation 
        set(selfInformation) {
            try {
                im.setConnectorByJsonLd(selfInformation)
            } catch (e: NullPointerException) {
                LOG.warn("Connector description build failed, building empty description.", e)
            }
        }

    /** Remove static connector profile based on JSON-LD data  */
    // TODO Document ApiOperation
    @DELETE
    @Path("/selfInformation")
    @Consumes("application/ld+json")
    @AuthorizationRequired
    fun removeSelfInformation() {
        try {
            im.setConnectorByJsonLd(null)
        } catch (e: NullPointerException) {
            LOG.warn("Connector description build failed, building empty description.", e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SettingsApi::class.java)
    }
}
