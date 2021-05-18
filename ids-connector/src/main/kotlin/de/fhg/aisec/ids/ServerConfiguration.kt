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

// import de.fhg.aisec.ids.dynamictls.AcmeSslContextFactory
// import org.eclipse.jetty.server.Connector
// import org.eclipse.jetty.server.Server
// import org.eclipse.jetty.server.ServerConnector
// import org.eclipse.jetty.util.ssl.SslContextFactory
// import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer
// import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
// import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Configuration
//
// @Configuration
// class ServerConfiguration {
//
//     @Bean
//     fun sslContextFactory(): SslContextFactory.Server {
//         return AcmeSslContextFactory()
//     }
//
//     @Bean
//     fun webServerFactory(sslContextFactory: SslContextFactory.Server): ConfigurableServletWebServerFactory {
//         return JettyServletWebServerFactory().apply {
//             port = 8443
//             serverCustomizers = listOf(
//                 JettyServerCustomizer { server: Server ->
//                     server.connectors = arrayOf<Connector>(
//                         ServerConnector(server, sslContextFactory)
//                     )
//                 }
//             )
//         }
//     }
// }
