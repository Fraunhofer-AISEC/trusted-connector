/*-
 * ========================LICENSE_START=================================
 * camel-ids
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
package de.fhg.aisec.ids.camel.ids.server;

import de.fhg.aisec.ids.comm.CertificatePair;
import java.security.cert.X509Certificate;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default websocket factory. Used when no custom websocket is needed. */
public class DefaultWebsocketFactory implements WebSocketFactory {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultWebsocketFactory.class);

  private final CertificatePair certificatePair;

  public DefaultWebsocketFactory(CertificatePair certificatePair) {
    this.certificatePair = certificatePair;
  }

  @Override
  public DefaultWebsocket newInstance(
      ServletUpgradeRequest request,
      String protocol,
      String pathSpec,
      NodeSynchronization sync,
      WebsocketConsumer consumer) {
    // Create final, complete pair from the local (server) certificate ...
    CertificatePair finalPair = new CertificatePair(certificatePair);
    // ... plus the remote (client) certificate from the request
    X509Certificate[] certificates = request.getCertificates();
    if (certificates != null && certificates.length > 0) {
      finalPair.setRemoteCertificate(certificates[0]);
    } else {
      LOG.warn("Remote client did not present TLS certificate");
    }
    return new DefaultWebsocket(sync, pathSpec, consumer, finalPair);
  }
}
