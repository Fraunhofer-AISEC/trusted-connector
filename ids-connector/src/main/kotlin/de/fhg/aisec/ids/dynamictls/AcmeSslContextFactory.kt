/*-
 * ========================LICENSE_START=================================
 * ids-connector
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
package de.fhg.aisec.ids.dynamictls

// import de.fhg.aisec.ids.api.acme.SslContextFactoryReloadable
// import de.fhg.aisec.ids.api.acme.SslContextFactoryReloadableRegistry
// import org.eclipse.jetty.util.ssl.SslContextFactory
// import org.slf4j.LoggerFactory
// import org.springframework.beans.factory.annotation.Autowired
//
// /**
//  * This SslContextFactory registers started instances to a service that allows reloading of
//  * all active SslContextFactory instances.
//  *
//  * @author Michael Lux
//  */
// class AcmeSslContextFactory : SslContextFactory.Server(), SslContextFactoryReloadable {
//
//     @Autowired
//     private lateinit var reloadableRegistry: SslContextFactoryReloadableRegistry
//
//     @Throws(Exception::class)
//     override fun doStart() {
//         if (LOG.isDebugEnabled) {
//             LOG.debug("Starting {}", this)
//         }
//         reloadableRegistry.registerSslContextFactoryReloadable(this)
//         super.doStart()
//     }
//
//     @Throws(java.lang.Exception::class)
//     override fun doStop() {
//         if (LOG.isDebugEnabled) {
//             LOG.debug("Stopping {}", this)
//         }
//         reloadableRegistry.removeSslContextFactoryReloadable(this)
//         super.doStop()
//     }
//
//     override fun reload(newKeyStorePath: String) {
//         try {
//             if (LOG.isInfoEnabled) {
//                 LOG.info("Reloading {}", this)
//             }
//             reload { f: SslContextFactory -> f.keyStorePath = newKeyStorePath }
//         } catch (e: Exception) {
//             LOG.error("Error whilst reloading SslContextFactory: $this", e)
//         }
//     }
//
//     override fun toString(): String {
//         return String.format(
//             "%s@%x (%s)", this.javaClass.simpleName, this.hashCode(), this.keyStorePath
//         )
//     }
//
//     companion object {
//         private val LOG = LoggerFactory.getLogger(AcmeSslContextFactory::class.java)
//     }
// }
