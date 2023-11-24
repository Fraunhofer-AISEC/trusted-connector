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
package de.fhg.aisec.ids.camel.processors.multipart

import org.apache.camel.Endpoint
import org.apache.camel.component.http.HttpEndpoint
import org.apache.camel.spi.EndpointStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("certExposingEndpointStrategy")
class CertExposingEndpointStrategy : EndpointStrategy {
    @Autowired
    private lateinit var certExposingHttpClientConfigurer: CertExposingHttpClientConfigurer

    override fun registerEndpoint(
        uri: String,
        endpoint: Endpoint
    ): Endpoint {
        if (endpoint is HttpEndpoint && uri.startsWith("https")) {
            LOG.info("Configured endpoint with uri $uri")
            endpoint.httpClientConfigurer = certExposingHttpClientConfigurer
        }
        return endpoint
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(CertExposingEndpointStrategy::class.java)
    }
}
