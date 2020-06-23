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
package de.fhg.aisec.ids.camel.ids.client;

import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.api.settings.Settings;
import de.fhg.aisec.ids.camel.ids.CamelComponent;
import de.fhg.aisec.ids.camel.ids.IdscpConstants;
import de.fhg.aisec.ids.camel.ids.ProxyX509TrustManager;
import de.fhg.aisec.ids.camel.ids.WebSocketConstants;
import de.fhg.aisec.ids.comm.CertificatePair;
import de.fhg.aisec.ids.comm.client.ClientConfiguration;
import de.fhg.aisec.ids.comm.client.IdspClientSocket;
import de.fhg.aisec.ids.comm.ws.protocol.IDSCPException;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.ahc.AhcEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.asynchttpclient.*;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This is the client-side implementation of a Camel endpoint for the IDS communication protocol
 * (IDCP).
 *
 * <p>It is based on camel-ahc, further info is available at: <a
 * href="http://github.com/sonatype/async-http-client">Async Http Client</a>.
 */
@UriEndpoint(
  scheme = "idsclientplain,idsclient",
  extendsScheme = "ahc,ahc",
  title = "IDS Protocol",
  syntax = "idsclient:httpUri",
  label = "websocket"
)
public class WsEndpoint extends AhcEndpoint {
  private static final Logger LOG = LoggerFactory.getLogger(WsEndpoint.class);

  private static final List<IDSCPOutgoingConnection> outgoingConnections = new ArrayList<>();

  private final Set<WsConsumer> consumers = new HashSet<>();
  private final WsListener listener = new WsListener(consumers, this);
  private WebSocket websocket;
  private final CertificatePair certificatePair = new CertificatePair();

  @UriParam(label = "producer")
  private boolean useStreaming;

  @UriParam(label = "consumer")
  private boolean sendMessageOnError;

  @UriParam(
    label = "attestation",
    defaultValue = "0",
    description =
        "defines the remote attestation mode: 0=BASIC, 1=ALL, 2=ADVANCED, 3=ZERO. default value is 0=BASIC. (see api/attestation.proto for more details)"
  )
  private int attestation = IdsAttestationType.BASIC.getNumber();

  @UriParam(
    label = "attestationMask",
    defaultValue = "0",
    description =
        "defines the upper boundary of PCR values tested in ADVANCED mode. i.e. attestationMask=5 means values PCR0, PCR1, PCR2, PCR3 and PCR4"
  )
  private Integer attestationMask = 0;

  public WsEndpoint(String endpointUri, WsComponent component) {
    super(endpointUri, component, null);
  }

  @Override
  public void setSslContextParameters(SSLContextParameters sslContextParameters) {
    if (sslContextParameters != null) {
      try {
        ProxyX509TrustManager.bindCertificatePair(sslContextParameters, false, certificatePair);
      } catch (GeneralSecurityException | IOException e) {
        LOG.error("Failed to patch TrustManager for WsEndpoint", e);
      }
    }
    super.setSslContextParameters(sslContextParameters);
  }

  @Override
  public WsComponent getComponent() {
    return (WsComponent) super.getComponent();
  }

  @Override
  public Producer createProducer() {
    return new WsProducer(this);
  }

  @Override
  public Consumer createConsumer(Processor processor) {
    return new WsConsumer(this, processor);
  }

  public static List<IDSCPOutgoingConnection> getOutgoingConnections() {
    return outgoingConnections;
  }

  WebSocket getWebSocket() {
    synchronized (this) {
      // ensure we are connected
      reConnect();
    }
    return websocket;
  }

  @SuppressWarnings("unused")
  void setWebSocket(WebSocket websocket) {
    this.websocket = websocket;
  }

  public boolean isUseStreaming() {
    return useStreaming;
  }

  /** To enable streaming to send data as multiple text fragments. */
  @SuppressWarnings("unused")
  public void setUseStreaming(boolean useStreaming) {
    this.useStreaming = useStreaming;
  }

  public boolean isSendMessageOnError() {
    return sendMessageOnError;
  }

  @SuppressWarnings("unused")
  public void setAttestation(int type) {
    this.attestation = type;
  }

  public int getAttestation() {
    return attestation;
  }

  @SuppressWarnings("unused")
  public void setAttestationMask(int type) {
    this.attestationMask = type;
  }

  public int getAttestationMask() {
    return attestationMask;
  }

  /** Whether to send an message if the web-socket listener received an error. */
  @SuppressWarnings("unused")
  public void setSendMessageOnError(boolean sendMessageOnError) {
    this.sendMessageOnError = sendMessageOnError;
  }

  @Override
  protected AsyncHttpClient createClient(AsyncHttpClientConfig config) {
    if (config == null) {
      config = new DefaultAsyncHttpClientConfig.Builder().build();
    }
    return new DefaultAsyncHttpClient(config);
  }

  public void connect() {
    String uri = getHttpUri().toASCIIString();
    if (uri.startsWith(IdscpConstants.WSS_PROTOCOL + ":")) {
      uri = uri.replaceFirst(IdscpConstants.WSS_PROTOCOL, WebSocketConstants.WSS_PROTOCOL);
    } else if (uri.startsWith(IdscpConstants.WS_PROTOCOL + ":")) {
      uri = uri.replaceFirst(IdscpConstants.WS_PROTOCOL, WebSocketConstants.WS_PROTOCOL);
    }

    LOG.debug("Connecting to {}", uri);
    BoundRequestBuilder reqBuilder =
        getClient().prepareGet(uri).addHeader("Sec-WebSocket-Protocol", "ids");

    LOG.debug("remote-attestation mode: {}", this.getAttestation());
    LOG.debug("remote-attestation mask: {}", this.getAttestationMask());

    IdsAttestationType attestationType = IdsAttestationType.forNumber(this.getAttestation());
    if (attestationType == null) {
      throw new RuntimeException("Error: " + this.getAttestation()
              + " does not seem to be a valid attestation type, aborting");
    }

    // Execute IDS protocol immediately after connect
    Settings settings = CamelComponent.getSettings();
    URI ttpUri = null;
    try {
      if (settings != null) {
        ttpUri =
            new URI(
                String.format(
                    "https://%s:%d/rat-verify",
                    settings.getConnectorConfig().getTtpHost(),
                    settings.getConnectorConfig().getTtpPort()));
      }
    } catch (URISyntaxException e) {
      LOG.error("incorrect TTP URI syntax", e);
    }
    if (ttpUri == null) {
      throw new RuntimeException("Error: ttpUri required, aborting!");
    }

    InfoModel infoModelManager = CamelComponent.getInfoModelManager();
    ClientConfiguration.Builder clientConfigBuilder =
        new ClientConfiguration.Builder()
            .attestationType(attestationType)
            .attestationMask(this.getAttestationMask())
            .certificatePair(certificatePair)
            .ttpUrl(ttpUri);
    if (infoModelManager == null) {
      clientConfigBuilder
          .rdfDescription("{\"message\":\"Infomodel is not available\"}")
          .dynamicAttributeToken("{\"message\":\"DAPS token is not available\"}");
    } else {
      try {
        clientConfigBuilder.rdfDescription(infoModelManager.getConnectorAsJsonLd());
      } catch (Exception x) {
        LOG.error("Infomodel load failed, please configure a valid Infomodel via the REST API!");
        clientConfigBuilder.rdfDescription("{\"message\":\"Infomodel is not available\"}");
      }
      try {
        clientConfigBuilder.dynamicAttributeToken(infoModelManager.getDynamicAttributeToken());
      } catch (Exception x) {
        LOG.error("DAPS token load failed, please verify your DAPS configuration!");
        clientConfigBuilder.dynamicAttributeToken("{\"message\":\"DAPS token is not available\"}");
      }
    }
    IdspClientSocket idspListener = new IdspClientSocket(clientConfigBuilder.build());

    try {
      // Block until IDSCP has finished
      idspListener.semaphore().lockInterruptibly();

      websocket =
          reqBuilder
              .execute(
                  new WebSocketUpgradeHandler.Builder().addWebSocketListener(idspListener).build())
              .get();

      do {
        try {
          if (idspListener.idscpInProgressCondition().await(30, TimeUnit.SECONDS)) {
            break;
          }
        } catch (InterruptedException ie) {
          LOG.warn("Interrupt occurred whilst waiting for IDSCP handshake", ie);
          Thread.currentThread().interrupt();
        }
      } while (!idspListener.isTerminated()); // To handle sporadic wake-ups
    } catch (ExecutionException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IDSCPException("Error in WebSocket connect", e);
    } finally {
      idspListener.semaphore().unlock();
    }
    // get remote address, get upgrade headers, instantiate ConnectionManagerService to register
    // websocket
    // When IDS protocol has finished, hand over to normal web socket listener
    websocket.addWebSocketListener(listener);
    websocket.removeWebSocketListener(idspListener);

    // Add Client Endpoint information to static List
    IDSCPOutgoingConnection ce = new IDSCPOutgoingConnection();
    ce.setAttestationResult(idspListener.getAttestationResult());
    ce.setMetaData(idspListener.getMetaResult());
    ce.setEndpointIdentifier(this.getEndpointUri());
    ce.setEndpointKey(this.getEndpointKey());
    ce.setRemoteIdentity(websocket.getRemoteAddress().toString());
    outgoingConnections.add(ce);
  }

  @Override
  protected void doStop() throws Exception {
    if (websocket != null && websocket.isOpen()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Disconnecting from {}", getHttpUri().toASCIIString());
      }
      websocket.removeWebSocketListener(listener);
      websocket.sendCloseFrame();
      websocket = null;
    }
    outgoingConnections.removeIf(ic -> ic.getEndpointKey().equals(this.getEndpointKey()));
    super.doStop();
  }

  void connect(WsConsumer wsConsumer) {
    consumers.add(wsConsumer);
    //reConnect();
  }

  void disconnect(WsConsumer wsConsumer) {
    consumers.remove(wsConsumer);
  }

  void reConnect() {
    if (websocket == null || !websocket.isOpen()) {
      String uri = getHttpUri().toASCIIString();
      LOG.info("Reconnecting websocket: {}", uri);
      connect();
    }
  }
}
