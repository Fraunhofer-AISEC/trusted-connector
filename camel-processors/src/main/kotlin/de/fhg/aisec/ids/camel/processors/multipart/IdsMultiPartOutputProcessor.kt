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
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.processors.multipart.MultiPartConstants.IDS_HEADER_KEY
import de.fhg.aisec.ids.camel.processors.multipart.MultiPartConstants.MEDIA_TYPE_JSON_LD
import de.fhg.aisec.ids.idscp2.api.drivers.DapsDriver
import de.fhg.aisec.ids.idscp2.api.error.DatException
import de.fraunhofer.iais.eis.DynamicAttributeToken
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder
import de.fraunhofer.iais.eis.Message
import de.fraunhofer.iais.eis.TokenFormat
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.xml.datatype.XMLGregorianCalendar

/**
 * The MultiPartOutputProcessor will read the Exchange's header "ids" (if present) and the
 * Exchange's body and emit a stream with a body that is a mime-multipart message with two parts of
 * content-type application/json: a "header" part containing the "ids" header and a "payload" part
 * containing the body.
 *
 * <p>The whole message is supposed to look like this: <code> --msgpart Content-Type:
 * application/json; charset=utf-8 Content-Disposition: form-data; name="header"
 *
 * { "@type" : "ids:ConnectorAvailableMessage", "id" :
 * "http://industrialdataspace.org/connectorAvailableMessage/34d761cf-5ca4-4a77-a7f4-b14d8f75636a",
 * "issued" : "2018-10-25T11:37:08.245Z", "modelVersion" : "1.0.1-SNAPSHOT", "issuerConnector" :
 * "https://companyA.com/connector/59a68243-dd96-4c8d-88a9-0f0e03e13b1b", "securityToken" : {
 * "@type" : "ids:Token", "id" :
 * "http://industrialdataspace.org/token/e43c08e1-157b-4207-94a8-754e53f48839", "tokenFormat" :
 * "https://w3id.org/idsa/code/tokenformat/JWT", "tokenValue" :
 * "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJEQVBTIERDIiwiYXVkIjoiSURTX0RDX0FTIiwic3ViIjoiREFQUyByZXNwb25zZSB0b2tlbiIsIm5hbWUiOiJEeW5hbWljIEF0dHJpYnV0ZXMgUHJvdmlzaW9uIFNlcnZpY2UgJ0RBUFMnIChlZGl0aW9uICdEZXZlbG9wZXJzIENvbW11bml0eScpIiwiZXhwIjoxNTQwMzkyMzU1MDI2LCJASURTIjp7IkBjb250ZXh0Ijp7IlNlY3VyaXR5UHJvZmlsZSI6Imh0dHBzOi8vc2NoZW1hLmluZHVzdHJpYWxkYXRhc3BhY2Uub3JnL3NlY3VyaXR5UHJvZmlsZS8iLCJEYXRhVXNhZ2VDb250cm9sIjoiaHR0cHM6Ly9zY2hlbWEuaW5kdXN0cmlhbGRhdGFzcGFjZS5vcmcvc2VjdXJpdHlQcm9maWxlL2RhdGFVc2FnZUNvbnRyb2xTdXBwb3J0In0sIkB0eXBlIjoicHJvZmlsZSIsInByb2ZpbGUiOnsiaWRzaWQiOiJodHRwOi8vd3d3Lm5pY29zLXJkLmNvbS9JRFMvQ29ubmVjdG9ycy9icm8tZGMiLCJAdHlwZSI6IlNlY3VyaXR5UHJvZmlsZSIsIlNlY3VyaXR5UHJvZmlsZSI6eyJpbnRlZ3JpdHlQcm90ZWN0aW9uQW5kVmVyaWZpY2F0aW9uU3VwcG9ydCI6Ik5PTkUiLCJhdXRoZW50aWNhdGlvblN1cHBvcnQiOiJOT05FIiwic2VydmljZUlzb2xhdGlvblN1cHBvcnQiOiJOT05FIiwiaW50ZWdyaXR5UHJvdGVjdGlvblNjb3BlIjoiTk9ORSIsImFwcEV4ZWN1dGlvblJlc291cmNlcyI6Ik5PTkUiLCJkYXRhVXNhZ2VDb250cm9sU3VwcG9ydCI6Ik5PTkUiLCJhdWRpdExvZ2dpbmciOiJOT05FIiwibG9jYWxEYXRhQ29uZmlkZW50aWFsaXR5IjoiTk9ORSJ9fX0sImlhdCI6MTU0MDM4ODc1NX0.3gEV3OB-Gf_Z4Hv6H5iQpC7OEoBvcEV887uOFjgC7jb1vBujT_qyVk3ETtZSEKUd2izChlE4MjskbhW-7I6dNvYfZz_6Hif9iH0dMsncPair5aph7vsPpH4V0AjiKXBqJrDqDGZeZxZuqcD6RmQjTvSDpVKj120xRbG_GgQg5jVJa427fkIS792vg078d7BlBfWUeYT3HBLE-fFNpQ6FIGA559O70TyXbxk-POkUWDE2cRm_fk-qvpTVDr79YYpPNuLc_0HgSzZJtXuT_Hn2hScrkYKCFJivsqI6f3Z1SvsGGIX5aTE_YSIEULScaRcq5M0u4ze1ynnnbtL4r59tig"
 * } } --msgpart Content-Type: application/json Content-Disposition: form-data; name="payload"
 *
 * { "@type" : "ids:BaseConnector", "id" :
 * "https://companyA.com/connector/59a68243-dd96-4c8d-88a9-0f0e03e13b1b", "defaultHost" : { "@type"
 * : "ids:Host", "id" : "http://industrialdataspace.org/host/73171c77-10d1-4e12-b04e-f696c467897e",
 * "protocol" : "https://w3id.org/idsa/code/protocol/HTTP", "accessUrl" :
 * "http://companyA.com/ids/connector" }, "curator" : "http://companyB.com/ids/participant",
 * "maintainer" : "http://companyA.com/ids/participant", "securityProfile" : { "@type" :
 * "ids:SecurityProfile", "id" :
 * "http://industrialdataspace.org/securityProfile/75f722e4-4165-4d06-b296-2cdfef39dca9", "basedOn"
 * : [ "PredefinedSecurityProfile", "https://w3id.org/idsa/core/Level0SecurityProfile" ] },
 * "inboundModelVersions" : [ "1.0.1-SNAPSHOT" ], "outboundModelVersion" : "1.0.1-SNAPSHOT",
 * "catalog" : { "@type" : "ids:Catalog", "id" :
 * "http://industrialdataspace.org/catalog/04b84cfd-b28c-4d22-b936-ee899900294a", "offers" : [ {
 * "@type" : "ids:Resource", "id" :
 * "http://industrialdataspace.org/resource/2e8ceebd-43f8-41c9-8225-a763fe83931a", "titles" : [ {
 * "@value" : "Quarterly Business Report", "@language" : "en" } ], "descriptions" : [ { "@value" :
 * "Dataset without retrieval interface and further description.", "@language" : "en" } ] } ] } }
 * --msgpart-- </code>
 *
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
@Component("idsMultiPartOutputProcessor")
class IdsMultiPartOutputProcessor : Processor {

    @Autowired
    lateinit var beanFactory: BeanFactory

    @Value("\${ids-multipart.daps-bean-name:}")
    var dapsBeanName: String? = null

    @Throws(Exception::class)
    override fun process(exchange: Exchange) {
        val boundary = UUID.randomUUID().toString()
        val multipartEntityBuilder = MultipartEntityBuilder.create()
        multipartEntityBuilder.setMode(HttpMultipartMode.STRICT)
        multipartEntityBuilder.setBoundary(boundary)

        val idsHeader = exchange.message.getHeader(IDS_HEADER_KEY)
            ?: throw RuntimeException("Required header \"ids-header\" not found, aborting.")

        val daps = dapsBeanName?.let { beanFactory.getBean(it, DapsDriver::class.java) }
            ?: run {
                LOG.warn("No DAPS instance has been specified, dummy DAT will be used!")
                null
            }

        // Our detection heuristic for an incomplete InfoModel idsHeader
        if (idsHeader::class.simpleName?.endsWith("Builder") == true) {
            try {
                if (LOG.isDebugEnabled) {
                    LOG.debug("Finalizing IDS idsHeader object...")
                }
                idsHeader.let {
                    it::class.java.apply {
                        getMethod("_securityToken_", DynamicAttributeToken::class.java)
                            .invoke(
                                it,
                                DynamicAttributeTokenBuilder()
                                    ._tokenFormat_(TokenFormat.JWT)
                                    ._tokenValue_(daps?.token?.toString(StandardCharsets.UTF_8) ?: "INVALID_DAT")
                                    .build()
                            )
                        getMethod("_senderAgent_", URI::class.java).invoke(it, Utils.senderAgentProducer())
                        getMethod("_issuerConnector_", URI::class.java).invoke(it, Utils.issuerProducer())
                        getMethod("_issued_", XMLGregorianCalendar::class.java)
                            .invoke(it, Utils.createGregorianCalendarTimestamp(System.currentTimeMillis()))
                        getMethod("_modelVersion_", String::class.java).invoke(it, Utils.infomodelVersion)
                        val message = getMethod("build").invoke(it)
                        if (message !is Message) {
                            throw RuntimeException(
                                "InfoModel message build failed! build() did not return a Message object!"
                            )
                        }
                        multipartEntityBuilder.addPart(
                            MultiPartConstants.MULTIPART_HEADER,
                            StringBody(SERIALIZER.serialize(message), MEDIA_TYPE_JSON_LD)
                        )
                    }
                }
            } catch (upa: UninitializedPropertyAccessException) {
                throw RuntimeException(
                    "At least one property of de.fhg.aisec.ids.camel.idscp2.Utils has not been " +
                        "properly initialized. This is a mandatory requirement for initialization " +
                        "of IDS Messages within IdsMultiPartOutputProcessor!",
                    upa
                )
            } catch (de: DatException) {
                throw RuntimeException(
                    "Error during retrieval of Dynamic Attribute Token (DAT)!",
                    de
                )
            } catch (t: Throwable) {
                throw RuntimeException(
                    "Failed to finalize idsHeader, the object must be an IDS MessageBuilder!",
                    t
                )
            }
        } else {
            multipartEntityBuilder.addPart(
                MultiPartConstants.MULTIPART_HEADER,
                StringBody(idsHeader.toString(), MEDIA_TYPE_JSON_LD)
            )
        }

        exchange.message.let {
            // Get the Exchange body and turn it into the second part named "payload"
            val contentTypeString = it.getHeader("Content-Type")?.toString()
            val payload = it.getBody(String::class.java)
            if (payload != null) {
                multipartEntityBuilder.addPart(
                    MultiPartConstants.MULTIPART_PAYLOAD,
                    StringBody(
                        payload,
                        if (contentTypeString == null) {
                            ContentType.create(
                                ContentType.TEXT_PLAIN.mimeType,
                                StandardCharsets.UTF_8
                            )
                        } else {
                            ContentType.parse(contentTypeString)
                        }
                    )
                )
            }

            // Remove current Content-Type header before setting the new one
            it.removeHeader("Content-Type")
            // Set Content-Type for multipart message
            it.setHeader("Content-Type", "multipart/mixed; boundary=$boundary")
            // Using InputStream as source for the message body
            it.body = multipartEntityBuilder.build().content
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IdsMultiPartOutputProcessor::class.java)
    }
}
