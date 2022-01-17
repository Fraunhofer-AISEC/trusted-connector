/*-
 * ========================LICENSE_START=================================
 * ids-webconsole
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole

import de.fhg.aisec.ids.webconsole.api.AppApi
import de.fhg.aisec.ids.webconsole.api.CORSResponseFilter
import de.fhg.aisec.ids.webconsole.api.CertApi
import de.fhg.aisec.ids.webconsole.api.ConfigApi
import de.fhg.aisec.ids.webconsole.api.ConnectionAPI
import de.fhg.aisec.ids.webconsole.api.JWTRestAPIFilter
import de.fhg.aisec.ids.webconsole.api.MetricAPI
import de.fhg.aisec.ids.webconsole.api.PolicyApi
import de.fhg.aisec.ids.webconsole.api.RouteApi
import de.fhg.aisec.ids.webconsole.api.SettingsApi
import de.fhg.aisec.ids.webconsole.api.UserApi
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.ws.rs.ApplicationPath

@Component
@ApplicationPath("cxf/api/v1")
class WebConsole : ResourceConfig() {
    private fun registerEndpoints() {
        LOG.info("Registering WebConsole classes")
        register(AppApi::class.java)
        register(CertApi::class.java)
        register(ConfigApi::class.java)
        register(ConnectionAPI::class.java)
        register(MetricAPI::class.java)
        register(PolicyApi::class.java)
        register(RouteApi::class.java)
        register(SettingsApi::class.java)
        register(UserApi::class.java)
        register(CORSResponseFilter::class.java)
        register(JWTRestAPIFilter::class.java)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WebConsole::class.java)
    }

    init {
        registerEndpoints()
    }
}
