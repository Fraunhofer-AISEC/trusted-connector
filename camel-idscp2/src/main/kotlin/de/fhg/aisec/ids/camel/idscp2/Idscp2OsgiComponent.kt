/*-
 * ========================LICENSE_START=================================
 * camel-ids
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
package de.fhg.aisec.ids.camel.idscp2

import de.fhg.aisec.ids.api.endpointconfig.EndpointConfigManager
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.api.tokenm.TokenManager
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ReferenceCardinality

@Component
class Idscp2OsgiComponent {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private lateinit var settings: Settings

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private lateinit var infoModelManager: InfoModel

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var tokenManager: TokenManager? = null

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private var endpointConfigManager: EndpointConfigManager? = null

    @Activate
    fun activate() {
        instance = this
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    fun setInfoModelManager(infoModelManager: InfoModel) {
        this.infoModelManager = infoModelManager
    }

    fun setTokenManager(tokenManager: TokenManager?) {
        this.tokenManager = tokenManager
    }

    fun setEndpointConfigManager(endpointConfigManager: EndpointConfigManager?) {
        this.endpointConfigManager = endpointConfigManager
    }

    companion object {
        private lateinit var instance: Idscp2OsgiComponent

        fun setInstance(instance: Idscp2OsgiComponent) {
            this.instance = instance
        }

        /**
         * Is never null due to ReferenceCardinality.MANDATORY,
         * but instance might be null for Unit Tests
         * @return Token-Manager instance
         */
        val settings get() = instance.settings

        /**
         * Is never null due to ReferenceCardinality.MANDATORY,
         * but instance might be null for Unit Tests
         * @return Info-Model-Manager instance
         */
        val infoModelManager get() = instance.infoModelManager

        val tokenManager get() = instance.tokenManager

        val endpointConfigManager get() = instance.endpointConfigManager
    }
}