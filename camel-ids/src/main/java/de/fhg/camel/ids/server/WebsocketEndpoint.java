/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fhg.camel.ids.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.server.Handler;

/**
 * The websocket component provides websocket endpoints for communicating with clients using websocket.
 *
 * This component uses Jetty as the websocket implementation.
 */
@UriEndpoint(scheme = "idsserver", title = "IDS Server Socket", syntax = "idsserver:host:port/resourceUri", consumerClass = WebsocketConsumer.class, label = "idsserver")
public class WebsocketEndpoint extends DefaultEndpoint {

    private WebsocketComponent component;
    private URI uri;
    private List<Handler> handlers;

    @UriPath(defaultValue = "0.0.0.0")
    private String host;
    @UriPath(defaultValue = "9292")
    private Integer port;
    @UriPath @Metadata(required = "true")
    private String resourceUri;

    @UriParam(label = "producer", defaultValue = "30000")
    private Integer sendTimeout = 30000;
    @UriParam(label = "consumer")
    private boolean sessionSupport;
    @UriParam(label = "cors", description = "enables or disables Cross Origin Filter")
    private boolean crossOriginFilterOn;
    @UriParam(label = "sslContextParameters", description = "used to save the SSLContextParameters when connecting via idsclient:// ")
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
    @UriParam(label = "attestation", defaultValue = "0", description = "defines the remote attestation mode: 0=BASIC, 1=ALL, 2=ADVANCED, 3=ZERO. default value is 0=BASIC. (see api/attestation.proto for more details)")
    private Integer attestation = 0;
    @UriParam(label = "attestationMask", defaultValue = "0", description = "defines the upper boundary of PCR values tested in ADVANCED mode. i.e. attestationMask=5 means values PCR0, PCR1, PCR2, PCR3 and PCR4")
    private Integer attestationMask = 0;

	public WebsocketEndpoint(WebsocketComponent component, String uri, String resourceUri, Map<String, Object> parameters) {
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
    public WebsocketComponent getComponent() {
        ObjectHelper.notNull(component, "component");
        return (WebsocketComponent) super.getComponent();
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        ObjectHelper.notNull(component, "component");
        WebsocketConsumer consumer = new WebsocketConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new WebsocketProducer(this);
    }

    public void connect(WebsocketConsumer consumer) throws Exception {
        component.connect(consumer);
    }

    public void disconnect(WebsocketConsumer consumer) throws Exception {
        component.disconnect(consumer);
    }

    public void connect(WebsocketProducer producer) throws Exception {
        component.connect(producer);
    }

    public void disconnect(WebsocketProducer producer) throws Exception {
        component.disconnect(producer);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

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
     * The hostname. The default value is <tt>0.0.0.0</tt>.
     * Setting this option on the component will use the component configured value as default.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * The port number. The default value is <tt>9292</tt>.
     * Setting this option on the component will use the component configured value as default.
     */
    public void setPort(int port) {
        this.port = port;
    }

    public Integer getSendTimeout() {
        return sendTimeout;
    }

    /**
     * Timeout in millis when sending to a websocket channel.
     * The default timeout is 30000 (30 seconds).
     */
    public void setSendTimeout(Integer sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public String getProtocol() {
        return uri.getScheme();
    }

    public String getPath() {
        return uri.getPath();
    }

    /**
     * Whether to enable session support which enables HttpSession for each http request.
     */
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

	public void setAttestation(int i) {
		attestation = i;
	}

    public int getAttestationMask() {
		return attestationMask;
	}

	public void setAttestationMask(int i) {
		attestationMask = i;
	}
    
    /**
     * Set the buffer size of the websocketServlet, which is also the max frame byte size (default 8192)
     */
    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Integer getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * Set the time in ms that the websocket created by the websocketServlet may be idle before closing. (default is 300000)
     */
    public void setMaxIdleTime(Integer maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public Integer getMaxTextMessageSize() {
        return maxTextMessageSize;
    }

    /**
     * Can be used to set the size in characters that the websocket created by the websocketServlet may be accept before closing.
     */
    public void setMaxTextMessageSize(Integer maxTextMessageSize) {
        this.maxTextMessageSize = maxTextMessageSize;
    }

    public Integer getMaxBinaryMessageSize() {
        return maxBinaryMessageSize;
    }

    /**
     * Can be used to set the size in bytes that the websocket created by the websocketServlet may be accept before closing. (Default is -1 - or unlimited)
     */
    public void setMaxBinaryMessageSize(Integer maxBinaryMessageSize) {
        this.maxBinaryMessageSize = maxBinaryMessageSize;
    }

    public Integer getMinVersion() {
        return minVersion;
    }

    /**
     * Can be used to set the minimum protocol version accepted for the websocketServlet. (Default 13 - the RFC6455 version)
     */
    public void setMinVersion(Integer minVersion) {
        this.minVersion = minVersion;
    }

    public List<Handler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<Handler> handlers) {
        this.handlers = handlers;
    }

    public SSLContextParameters getSslContextParameters() {
        return sslContextParameters;
    }

    /**
     * To configure security using SSLContextParameters
     */
    public void setSslContextParameters(SSLContextParameters sslContextParameters) {
        this.sslContextParameters = sslContextParameters;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * The CORS allowed origins. Use * to allow all.
     */
    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public boolean isCrossOriginFilterOn() {
        return crossOriginFilterOn;
    }

    /**
     * Whether to enable CORS
     */
    public void setCrossOriginFilterOn(boolean crossOriginFilterOn) {
        this.crossOriginFilterOn = crossOriginFilterOn;
    }

    public String getFilterPath() {
        return filterPath;
    }

    /**
     * Context path for filtering CORS
     */
    public void setFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    /**
     * Name of the websocket channel to use
     */
    public void setResourceUri(String resourceUri) {
        this.resourceUri = resourceUri;
    }
}
