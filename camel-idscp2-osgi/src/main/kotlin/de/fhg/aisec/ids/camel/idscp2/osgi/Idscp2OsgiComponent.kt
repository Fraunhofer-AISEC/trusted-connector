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
package de.fhg.aisec.ids.camel.idscp2.osgi

import de.fhg.aisec.ids.api.idscp2.Idscp2UsageControlInterface
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.camel.idscp2.UsageControlMaps
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.informationmodelmanager.BuildConfig
import org.apache.camel.Exchange
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ReferenceCardinality
import java.net.URI

@Suppress("unused")
@Component
class Idscp2OsgiComponent : Idscp2UsageControlInterface {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private lateinit var settings: Settings

    init {
        Utils.connectorUrlProducer = { settings.connectorProfile.connectorUrl }
        Utils.maintainerUrlProducer = { settings.connectorProfile.maintainerUrl }
        Utils.dapsUrlProducer = { settings.connectorConfig.dapsUrl }
        Utils.infomodelVersion = BuildConfig.INFOMODEL_VERSION
    }

    @Activate
    fun activate() {
        instance = this
    }

    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    override fun getExchangeContract(exchange: Exchange) =
            UsageControlMaps.getExchangeContract(exchange)

    override fun isProtected(exchange: Exchange) = UsageControlMaps.isProtected(exchange)

    override fun protectBody(exchange: Exchange, contractUri: URI) =
            UsageControlMaps.protectBody(exchange, contractUri)

    override fun unprotectBody(exchange: Exchange) = UsageControlMaps.unprotectBody(exchange)

    companion object {
        private lateinit var instance: Idscp2OsgiComponent

        fun setInstance(instance: Idscp2OsgiComponent) {
            Companion.instance = instance
        }
    }
}