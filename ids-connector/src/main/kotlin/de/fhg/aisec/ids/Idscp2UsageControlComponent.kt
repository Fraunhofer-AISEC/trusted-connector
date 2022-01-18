/*-
 * ========================LICENSE_START=================================
 * ids-connector
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
package de.fhg.aisec.ids

import com.google.common.collect.MapMaker
import de.fhg.aisec.ids.api.idscp2.Idscp2UsageControlInterface
import de.fhg.aisec.ids.camel.processors.UsageControlMaps
import org.apache.camel.Exchange
import org.springframework.stereotype.Component
import java.net.URI

@Component
class Idscp2UsageControlComponent : Idscp2UsageControlInterface {
    override fun getExchangeContract(exchange: Exchange) =
        UsageControlMaps.getExchangeContract(exchange)

    override fun protectBody(exchange: Exchange, contractUri: URI) {
        protectedBodies[exchange] = exchange.message.body
        exchange.message.body = "### Usage control protected body, contract $contractUri ###"
    }

    override fun isProtected(exchange: Exchange) = protectedBodies.containsKey(exchange)

    override fun unprotectBody(exchange: Exchange) {
        exchange.message.body = protectedBodies[exchange]
        protectedBodies -= exchange
    }

    companion object {
        private val protectedBodies: MutableMap<Exchange, Any> =
            MapMaker().weakKeys().makeMap()
    }
}
