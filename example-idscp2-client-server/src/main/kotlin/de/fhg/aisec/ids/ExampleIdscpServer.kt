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

import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.support.jsse.KeyManagersParameters
import org.apache.camel.support.jsse.KeyStoreParameters
import org.apache.camel.support.jsse.SSLContextParameters
import org.apache.camel.support.jsse.TrustManagersParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
open class ExampleIdscpServer {

    @Bean("serverSslContext")
    open fun createSSLContext(): SSLContextParameters {
        val ctx = SSLContextParameters()
        ctx.certAlias = "1.0.1"
        ctx.keyManagers = KeyManagersParameters()
        ctx.keyManagers.keyStore = KeyStoreParameters()
        ctx.keyManagers.keyStore.resource =
            File(
                Thread.currentThread()
                    .contextClassLoader
                    //.getResource("etc/consumer-core-protocol-test.p12")
                    .getResource("etc/consumer-keystore.p12")
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
            ).path
        ctx.trustManagers.keyStore.password = "password"

        return ctx
    }

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
