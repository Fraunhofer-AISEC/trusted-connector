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

import de.fhg.aisec.ids.camel.ids.WebSocketConstants;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.jetty.server.Handler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * The websocket component provides websocket endpoints for communicating with clients using
 * websocket.
 *
 * <p>This component uses Jetty as the websocket implementation.
 */
@UriEndpoint(
  scheme = "idsserver",
  title = "IDS Server Socket",
  syntax = "idsserver:host:port/resourceUri",
  label = "idsserver"
)
public class WebSocketEndpoint extends DefaultEndpoint {

  private final WebSocketComponent component;
  private final URI uri;
  private List<Handler> handlers;

  @UriPath(defaultValue = WebSocketConstants.DEFAULT_HOST)
  private String host;

  @UriPath(defaultValue = WebSocketConstants.DEFAULT_PORT)
  private Integer port;

  @UriPath
  @Metadata(required = true)
  private String resourceUri;

  @UriParam(label = "producer", defaultValue = "30000")
  private Integer sendTimeout = 30000;

  @UriParam(label = "consumer")
  private boolean sessionSupport;

  @UriParam(label = "cors", description = "enables or disables Cross Origin Filter")
  private boolean crossOriginFilterOn;

  @UriParam(
    label = "sslContextParameters",
    description = "used to save the SSLContextParameters when connecting via idsclient:// "
  )
  private SSLContextParameters sslContextParameters;

  @UriParam(label = "cors")
  private String allowedOrigins;

  @UriParam(label = "cors")
  private String filterPath;

  @UriParam(label = "advanced", defaultValue = "8192")
  private Integer bufferSize;

  @UriParam(label = "advanced", defaultValue = "300000")
  private Integer maxIdleTime;

  @UriParam(label = "advanced")
  private Integer maxTextMessageSize;

  @UriParam(defaultValue = "-1")
  private Integer maxBinaryMessageSize;

  @UriParam(label = "advanced", defaultValue = "13")
  private Integer minVersion;

  @UriParam(
    label = "attestation",
    defaultValue = "0",
    description =
        "defines the remote attestation mode: 0=BASIC, 1=ALL, 2=ADVANCED, 3=ZERO. default value is 0=BASIC. (see api/attestation.proto for more details)"
  )
  private Integer attestation = 0;

  @UriParam(
    label = "attestationMask",
    defaultValue = "0",
    description =
        "defines the upper boundary of PCR values tested in ADVANCED mode. i.e. attestationMask=5 means values PCR0, PCR1, PCR2, PCR3 and PCR4"
  )
  private Integer attestationMask = 0;

  public WebSocketEndpoint(
          WebSocketComponent component,
          String uri,
          String resourceUri) {
    super(uri, component);
    this.resourceUri = resourceUri;
    this.component = component;
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public WebSocketComponent getComponent() {
    ObjectHelper.notNull(component, "component");
    return (WebSocketComponent) super.getComponent();
  }

  @Override
  public Consumer createConsumer(Processor processor) throws Exception {
    ObjectHelper.notNull(component, "component");
    WebSocketConsumer consumer = new WebSocketConsumer(this, processor);
    configureConsumer(consumer);
    return consumer;
  }

  @Override
  public Producer createProducer() {
    return new WebSocketProducer(this);
  }

  public void connect(WebSocketConsumer consumer) throws Exception {
    component.connect(consumer);
  }

  public void disconnect(WebSocketConsumer consumer) throws Exception {
    component.disconnect(consumer);
  }

  public void connect(WebSocketProducer producer) throws Exception {
    component.connect(producer);
  }

  public void disconnect(WebSocketProducer producer) throws Exception {
    component.disconnect(producer);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @SuppressWarnings("unused")
  public URI getUri() {
    return uri;
  }

  public Integer getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  /**
   * The hostname. The default value is <tt>0.0.0.0</tt>. Setting this option on the component will
   * use the component configured value as default.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * The port number. The default value is <tt>9292</tt>. Setting this option on the component will
   * use the component configured value as default.
   */
  public void setPort(int port) {
    this.port = port;
  }

  public Integer getSendTimeout() {
    return sendTimeout;
  }

  /**
   * Timeout in millis when sending to a websocket channel. The default timeout is 30000 (30
   * seconds).
   */
  @SuppressWarnings("unused")
  public void setSendTimeout(Integer sendTimeout) {
    this.sendTimeout = sendTimeout;
  }

  public String getProtocol() {
    return uri.getScheme();
  }

  @SuppressWarnings("unused")
  public String getPath() {
    return uri.getPath();
  }

  /** Whether to enable session support which enables HttpSession for each http request. */
  @SuppressWarnings("unused")
  public void setSessionSupport(boolean support) {
    sessionSupport = support;
  }

  public boolean isSessionSupport() {
    return sessionSupport;
  }

  public Integer getBufferSize() {
    return bufferSize;
  }

  public int getAttestation() {
    return attestation;
  }

  @SuppressWarnings("unused")
  public void setAttestation(int i) {
    attestation = i;
  }

  public int getAttestationMask() {
    return attestationMask;
  }

  @SuppressWarnings("unused")
  public void setAttestationMask(int i) {
    attestationMask = i;
  }

  /**
   * Set the buffer size of the websocketServlet, which is also the max frame byte size (default
   * 8192)
   */
  @SuppressWarnings("unused")
  public void setBufferSize(Integer bufferSize) {
    this.bufferSize = bufferSize;
  }

  public Integer getMaxIdleTime() {
    return maxIdleTime;
  }

  /**
   * Set the time in ms that the websocket created by the websocketServlet may be idle before
   * closing. (default is 300000)
   */
  @SuppressWarnings("unused")
  public void setMaxIdleTime(Integer maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  public Integer getMaxTextMessageSize() {
    return maxTextMessageSize;
  }

  /**
   * Can be used to set the size in characters that the websocket created by the websocketServlet
   * may be accept before closing.
   */
  @SuppressWarnings("unused")
  public void setMaxTextMessageSize(Integer maxTextMessageSize) {
    this.maxTextMessageSize = maxTextMessageSize;
  }

  public Integer getMaxBinaryMessageSize() {
    return maxBinaryMessageSize;
  }

  /**
   * Can be used to set the size in bytes that the websocket created by the websocketServlet may be
   * accept before closing. (Default is -1 - or unlimited)
   */
  @SuppressWarnings("unused")
  public void setMaxBinaryMessageSize(Integer maxBinaryMessageSize) {
    this.maxBinaryMessageSize = maxBinaryMessageSize;
  }

  public Integer getMinVersion() {
    return minVersion;
  }

  /**
   * Can be used to set the minimum protocol version accepted for the websocketServlet. (Default 13
   * - the RFC6455 version)
   */
  @SuppressWarnings("unused")
  public void setMinVersion(Integer minVersion) {
    this.minVersion = minVersion;
  }

  public List<Handler> getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public void setHandlers(List<Handler> handlers) {
    this.handlers = handlers;
  }

  public SSLContextParameters getSslContextParameters() {
    return sslContextParameters;
  }

  /** To configure security using SSLContextParameters */
  public void setSslContextParameters(SSLContextParameters sslContextParameters) {
    this.sslContextParameters = sslContextParameters;
  }

  public String getAllowedOrigins() {
    return allowedOrigins;
  }

  /** The CORS allowed origins. Use * to allow all. */
  @SuppressWarnings("unused")
  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public boolean isCrossOriginFilterOn() {
    return crossOriginFilterOn;
  }

  /** Whether to enable CORS */
  @SuppressWarnings("unused")
  public void setCrossOriginFilterOn(boolean crossOriginFilterOn) {
    this.crossOriginFilterOn = crossOriginFilterOn;
  }

  public String getFilterPath() {
    return filterPath;
  }

  /** Context path for filtering CORS */
  @SuppressWarnings("unused")
  public void setFilterPath(String filterPath) {
    this.filterPath = filterPath;
  }

  public String getResourceUri() {
    return resourceUri;
  }

  /** Name of the websocket channel to use */
  @SuppressWarnings("unused")
  public void setResourceUri(String resourceUri) {
    this.resourceUri = resourceUri;
  }
}
