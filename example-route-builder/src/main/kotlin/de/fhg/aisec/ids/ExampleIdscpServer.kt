/*-
 * ========================LICENSE_START=================================
 * example-route-builder
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

import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ExampleIdscpServer {

    @Bean("serverSslContext")
    open fun createSSLContext() = ExampleConnector.getSslContext()

    @Bean
    open fun server(): RoutesBuilder {
        return object : RouteBuilder() {
            override fun configure() {
                from("idscp2server://0.0.0.0:29292?sslContextParameters=#serverSslContext")
                    .log("Server received: \${body} (Header: \${headers[idscp2-header]})")
                    .setBody().simple("PONG")
                    .setHeader("idscp2-header").simple("pong")
                    .log("Server response: \${body} (Header: \${headers[idscp2-header]})")
            }
        }
    }
}
