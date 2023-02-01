/*-
 * ========================LICENSE_START=================================
 * camel-processors
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
package de.fhg.aisec.ids.camel.processors.multipart

import de.fhg.aisec.ids.api.contracts.ContractUtils.SERIALIZER
import de.fhg.aisec.ids.camel.processors.UsageControlMaps
import de.fhg.aisec.ids.idscp2.api.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.api.sha256Fingerprint
import de.fraunhofer.iais.eis.Message
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.eclipse.jetty.server.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession

@Component("idsMultiPartInputProcessor")
class IdsMultiPartInputProcessor : Processor {

    @Autowired
    lateinit var beanFactory: BeanFactory

    @Value("\${ids-multipart.daps-bean-name:}")
    var dapsBeanName: String? = null

    @Throws(Exception::class)
    override fun process(exchange: Exchange) {
        exchange.message.let { message ->
            // Parse Multipart message
            val parser = MultiPartStringParser(message.getBody(InputStream::class.java))
            // Parse IDS header (should be an InfoModel Message object)
            val idsHeader = parser.header?.let { header ->
                SERIALIZER.deserialize(header, Message::class.java).also {
                    message.setHeader(MultiPartConstants.IDS_HEADER_KEY, it)
                }
            } ?: throw RuntimeException("No IDS header found!")

            val dat = idsHeader.securityToken?.tokenValue ?: throw RuntimeException("No DAT provided!")

            dapsBeanName?.let { dapsBeanName ->
                val peerCertificateHash: String = if (message.headers.containsKey("CamelHttpServletRequest")) {
                    // Assume server-side REST endpoint.
                    // Try to extract certificates from CamelHttpServletRequest reference.
                    val request = message.headers["CamelHttpServletRequest"] as Request
                    val sslSession = request.getAttribute("org.eclipse.jetty.servlet.request.ssl_session") as SSLSession
                    try {
                        sslSession.peerCertificates[0].sha256Fingerprint
                    } catch (e: SSLPeerUnverifiedException) {
                        LOG.error("Client didn't provide a certificate!")
                        throw e
                    }
                } else {
                    // Assume client-side HTTPS request.
                    // Try to obtain Certificate hash extracted by CertExposingHttpClientConfigurer.
                    message.headers[CertExposingHttpClientConfigurer.SERVER_CERTIFICATE_HASH_HEADER]?.toString()
                        ?: throw RuntimeException(
                            "Could not obtain server TLS certificate! Has CertExposingHttpClientConfigurer been invoked?"
                        )
                }
                if (LOG.isTraceEnabled) {
                    LOG.trace("Peer Certificate hash: {}", peerCertificateHash)
                }
                val daps = beanFactory.getBean(dapsBeanName, DapsDriver::class.java)
                try {
                    val verifiedDat = daps.verifyToken(dat.toByteArray(), peerCertificateHash)
                    // Save exchange peer identity for contract association
                    UsageControlMaps.setExchangePeerIdentity(exchange, verifiedDat.identity)
                    // Save effective transfer contract for peer
                    UsageControlMaps.setPeerContract(verifiedDat.identity, idsHeader.transferContract)
                } catch (e: Exception) {
                    throw SecurityException("Access Token did not match presented certificate!", e)
                }
            } ?: LOG.warn("No DAPS instance has been specified, DAT is not checked!")

            // Extract DAT from IDS header and assemble auth header
            exchange.getIn().setHeader("dat", "Bearer $dat")

            // Remove current Content-Type header before setting the new one
            message.removeHeader("Content-Type")
            // Copy Content-Type from payload part
            message.setHeader("Content-Type", parser.payloadContentType)
            // Populate body with extracted payload
            message.body = parser.payload
        }
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(IdsMultiPartInputProcessor::class.java)
    }
}
