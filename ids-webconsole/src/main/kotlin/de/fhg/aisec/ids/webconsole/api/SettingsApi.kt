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
import de.fhg.aisec.ids.webconsole.ApiController
import de.fraunhofer.iais.eis.util.TypedLiteral
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Authorization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import javax.ws.rs.core.MediaType

/**
 * REST API interface for Connector settings in the connector.
 *
 *
 * The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
</method> */
@ApiController
@RequestMapping("/settings")
@Api(value = "Self-Description and Connector Profiles", authorizations = [Authorization(value = "oauth2")])
class SettingsApi {

    @Autowired
    private lateinit var im: InfoModel

    @PostMapping("/connectorProfile", consumes = [MediaType.APPLICATION_JSON])
    @ApiOperation(value = "Configure the connector's self-description (\"Connector Profile\").")
    fun postConnectorProfile(@RequestBody profile: ConnectorProfile) {
        if (!im.setConnector(profile)) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while storing ConnectorProfile")
        }
    }

    /** Returns Connector profile based on currently stored preferences or empty Connector profile  */
    @ApiOperation(
        value = "Returns this connector's self-description (\"Connector Profile\")",
        response = ConnectorProfile::class
    )
    @GetMapping("/connectorProfile", produces = [MediaType.APPLICATION_JSON])
    fun getConnectorProfile(): ConnectorProfile {
        val c = im.connector
        return if (c == null) {
            ConnectorProfile()
        } else {
            ConnectorProfile(
                c.securityProfile,
                c.id,
                c.maintainer,
                c.description.map { obj: Any? -> TypedLiteral::class.java.cast(obj) }
            )
        }
    }

    /**
     * Returns connector profile based on currently stored preferences or statically provided JSON-LD
     * model, or empty connector profile if none of those are available.
     */
    @get:GetMapping("/selfInformation", produces = ["application/ld+json"])
    @set:PostMapping("/selfInformation", consumes = ["application/ld+json"])
    var selfInformation: String
        // TODO Document ApiOperation
        get() = try {
            im.connectorAsJsonLd
        } catch (e: Throwable) {
            LOG.error("Connector description build failed", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Connector description build failed", e)
        }

        // TODO Document ApiOperation
        set(@RequestBody selfInformation) {
            try {
                im.setConnectorByJsonLd(selfInformation)
            } catch (e: NullPointerException) {
                LOG.warn("Connector description build failed, building empty description.", e)
            }
        }

    /** Remove static connector profile based on JSON-LD data  */
    // TODO Document ApiOperation
    @DeleteMapping("/selfInformation")
    fun removeSelfInformation() {
        try {
            im.setConnectorByJsonLd(null)
        } catch (e: Throwable) {
            LOG.error("Connector description build failed", e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SettingsApi::class.java)
    }
}
