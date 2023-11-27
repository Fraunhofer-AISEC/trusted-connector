/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2023 Fraunhofer AISEC
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
package de.fhg.aisec.ids.camel.processors

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * This processor configures the shutdown timeout of the ShutdownStrategy in this context.
 */
@Component("shutdownConfigurationProcessor")
class ShutdownConfigurationProcessor : Processor {
    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }
        exchange.getProperty("timeout-seconds")?.let {
            val seconds = it.toString().toLong()
            if (LOG.isDebugEnabled) {
                LOG.debug("Setting shutdown timeout to {} seconds...", seconds)
            }
            exchange.context.shutdownStrategy.timeout = seconds
        } ?: LOG.warn("Property \"timeout-seconds\" not found, no change will be performed.")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ShutdownConfigurationProcessor::class.java)
    }
}
