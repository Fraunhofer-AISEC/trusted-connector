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

import org.apache.commons.fileupload.FileUpload
import org.apache.commons.fileupload.UploadContext
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class MultiPartStringParser internal constructor(
    private val multipartInput: InputStream
) : UploadContext {
    private var boundary: String? = null
    var header: String? = null
    var payload: InputStream? = null
    var payloadContentType: String? = null

    override fun getCharacterEncoding(): String = StandardCharsets.UTF_8.name()

    @Deprecated(
        "Deprecated in favor of contentLength(), see parent class org.apache.commons.fileupload.UploadContext",
        ReplaceWith("contentLength()")
    )
    override fun getContentLength() = -1

    override fun getContentType() = "multipart/form-data, boundary=$boundary"

    override fun getInputStream() = multipartInput

    override fun contentLength() = -1L

    companion object {
        private val LOG = LoggerFactory.getLogger(MultiPartStringParser::class.java)
    }

    init {
        multipartInput.mark(10240)
        BufferedReader(InputStreamReader(multipartInput, StandardCharsets.UTF_8)).use { reader ->
            val boundaryLine =
                reader.readLine()
                    ?: throw IOException(
                        "Message body appears to be empty, expected multipart boundary."
                    )
            boundary = boundaryLine.substring(2).trim { it <= ' ' }
            multipartInput.reset()
            for (i in FileUpload(DiskFileItemFactory()).parseRequest(this)) {
                val fieldName = i.fieldName
                if (LOG.isTraceEnabled) {
                    LOG.trace("Found multipart field with name \"{}\"", fieldName)
                }
                if (MultiPartConstants.MULTIPART_HEADER == fieldName) {
                    header = i.string
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Found header:\n{}", header)
                    }
                } else if (MultiPartConstants.MULTIPART_PAYLOAD == fieldName) {
                    payload = i.inputStream
                    payloadContentType = i.contentType
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Found body with Content-Type \"{}\"", payloadContentType)
                    }
                } else {
                    throw IOException("Unknown multipart field name detected: $fieldName")
                }
            }
        }
    }
}
