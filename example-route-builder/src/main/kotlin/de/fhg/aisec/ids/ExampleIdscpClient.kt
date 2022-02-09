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
open class ExampleIdscpClient {

    @Bean("clientSslContext")
    open fun createSSLContext() = ExampleConnector.getSslContext()

    @Bean
    open fun client(): RoutesBuilder {
        return object : RouteBuilder() {
            override fun configure() {
                from("timer://tenSecondsTimer?fixedRate=true&period=10000")
                    .setBody().simple("PING")
                    .setHeader("idscp2-header").simple("ping")
                    .log("Client sends: \${body} (Header: \${headers[idscp2-header]})")
                    .to("idscp2client://consumer-core:29292?awaitResponse=true&sslContextParameters=#clientSslContext")
                    .log("Client received: \${body} (Header: \${headers[idscp2-header]})")
                    .removeHeader("idscp2-header") // Prevents client consumer from sending the message back to the server
                    .setBody().simple("\${null}")
            }
        }
    }
}
