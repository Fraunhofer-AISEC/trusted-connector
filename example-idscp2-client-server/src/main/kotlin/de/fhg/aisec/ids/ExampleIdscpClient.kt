/*-
 * ========================LICENSE_START=================================
 * example-idscp2-client-server
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

import java.io.File
import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.support.jsse.KeyManagersParameters
import org.apache.camel.support.jsse.KeyStoreParameters
import org.apache.camel.support.jsse.SSLContextParameters
import org.apache.camel.support.jsse.TrustManagersParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ExampleIdscpClient {

    @Bean("clientSslContext")
    open fun createSSLContext(): SSLContextParameters {
        val ctx = SSLContextParameters()
        ctx.certAlias = "1.0.1"
        ctx.keyManagers = KeyManagersParameters()
        ctx.keyManagers.keyStore = KeyStoreParameters()
        ctx.keyManagers.keyStore.resource =
            File(
                    Thread.currentThread()
                        .contextClassLoader
                        .getResource("etc/provider-core-protocol-test.p12")
                        .path
                )
                .path
        ctx.keyManagers.keyStore.password = "password"
        ctx.trustManagers = TrustManagersParameters()
        ctx.trustManagers.keyStore = KeyStoreParameters()
        ctx.trustManagers.keyStore.resource =
            File(
                    Thread.currentThread()
                        .contextClassLoader
                        .getResource("etc/truststore.p12")
                        .path
                )
                .path
        ctx.trustManagers.keyStore.password = "password"

        return ctx
    }

    @Bean
    open fun client(): RoutesBuilder {
        return object : RouteBuilder() {
            override fun configure() {
                from("timer://tenSecondsTimer?fixedRate=true&period=10000")
                    .setBody()
                        .simple("PING")
                    .setHeader("idscp2-header")
                        .simple("ping")
                    .log("Client sends: \${body} (Header: \${headers[idscp2-header]})")
                    .to(
                        "idscp2client://localhost:29292?awaitResponse=true&sslContextParameters=#clientSslContext"
                    )
                    .log("Client received: \${body} (Header: \${headers[idscp2-header]})")
                    .removeHeader(
                        "idscp2-header"
                    ) // Prevents the client consumer from sending the message back to the server
                    .setBody()
                        .simple("\${null}")
            }
        }
    }

}
