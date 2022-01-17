/*-
 * ========================LICENSE_START=================================
 * camel-multipart-processor
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
package de.fhg.aisec.ids.camel.multipart

import de.fhg.aisec.ids.api.infomodel.InfoModel
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

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
class MultiPartOutputProcessor : Processor {
    @Throws(Exception::class)
    override fun process(exchange: Exchange) {
        val boundary = UUID.randomUUID().toString()
        val multipartEntityBuilder = MultipartEntityBuilder.create()
        multipartEntityBuilder.setMode(HttpMultipartMode.STRICT)
        multipartEntityBuilder.setBoundary(boundary)

        // Get the IDS InfoModelManager and retrieve a JSON-LD-serialized self-description that
        // will be sent as a multipart "header"
        val infoModel: InfoModel = MultiPartComponent.infoModelManager
        val rdfHeader = infoModel.connectorAsJsonLd

        // Use the self-description provided by the InfoModelManager as "header"
        multipartEntityBuilder.addPart(
            MultiPartConstants.MULTIPART_HEADER,
            StringBody(rdfHeader, ContentType.APPLICATION_JSON)
        )

        exchange.message.let {
            // Get the Exchange body and turn it into the second part named "payload"
            val contentTypeString = it.getHeader("Content-Type")
            if (contentTypeString != null) {
                val payload = it.getBody(InputStream::class.java)
                if (payload != null) {
                    multipartEntityBuilder.addPart(
                        MultiPartConstants.MULTIPART_PAYLOAD,
                        InputStreamBody(
                            payload,
                            ContentType.create(contentTypeString.toString().split(";").first())
                        )
                    )
                }
            } else {
                val payload = it.getBody(String::class.java)
                if (payload != null) {
                    multipartEntityBuilder.addPart(
                        MultiPartConstants.MULTIPART_PAYLOAD,
                        StringBody(
                            payload,
                            ContentType.create(
                                ContentType.TEXT_PLAIN.mimeType,
                                StandardCharsets.UTF_8
                            )
                        )
                    )
                }
            }

            // Remove current Content-Type header before setting the new one
            it.removeHeader("Content-Type")
            // Set Content-Type for multipart message
            it.setHeader("Content-Type", "multipart/mixed; boundary=$boundary")
            // Using InputStream as source for the message body
            it.body = multipartEntityBuilder.build().content
        }
    }
}
