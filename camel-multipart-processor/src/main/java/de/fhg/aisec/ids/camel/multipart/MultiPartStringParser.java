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
package de.fhg.aisec.ids.camel.multipart;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static de.fhg.aisec.ids.camel.multipart.MultiPartConstants.MULTIPART_HEADER;
import static de.fhg.aisec.ids.camel.multipart.MultiPartConstants.MULTIPART_PAYLOAD;

public class MultiPartStringParser implements UploadContext {

  private static final Logger LOG = LoggerFactory.getLogger(MultiPartStringParser.class);
  private final InputStream multipartInput;
  private final String boundary;
  private String header;
  private InputStream payload;
  private String payloadContentType;

  MultiPartStringParser(final InputStream multipartInput) throws FileUploadException, IOException {
    this.multipartInput = multipartInput;
    multipartInput.mark(10240);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(multipartInput, StandardCharsets.UTF_8))) {
      String boundaryLine = reader.readLine();
      if (boundaryLine == null) {
        throw new IOException("Message body appears to be empty, expected multipart boundary.");
      }
      this.boundary = boundaryLine.substring(2).trim();
      this.multipartInput.reset();
      for (FileItem i : new FileUpload(new DiskFileItemFactory()).parseRequest(this)) {
        String fieldName = i.getFieldName();
        if (LOG.isTraceEnabled()) {
          LOG.trace("Found multipart field with name \"{}\"", fieldName);
        }
        if (MULTIPART_HEADER.equals(fieldName)) {
          header = i.getString();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Found header:\n{}", header);
          }
        } else if (MULTIPART_PAYLOAD.equals(fieldName)) {
          payload = i.getInputStream();
          payloadContentType = i.getContentType();
          if (LOG.isDebugEnabled()) {
            LOG.debug("Found body with Content-Type \"{}\"", payloadContentType);
          }
        } else {
          throw new IOException("Unknown multipart field name detected: " + fieldName);
        }
      }
    }
  }

  @Override
  public String getCharacterEncoding() {
    return StandardCharsets.UTF_8.name();
  }

  @Override
  public int getContentLength() {
    return -1;
  }

  @Override
  public String getContentType() {
    return "multipart/form-data, boundary=" + this.boundary;
  }

  @Override
  public InputStream getInputStream() {
    return multipartInput;
  }

  @Override
  public long contentLength() {
    return -1;
  }

  public String getHeader() {
    return header;
  }

  public InputStream getPayload() {
    return payload;
  }

  public String getPayloadContentType() {
    return payloadContentType;
  }
}
