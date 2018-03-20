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
package de.fhg.camel.ids.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.ahc.AhcEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ws.DefaultWebSocketListener;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPClientEndpoint;
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection;
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection;
import de.fhg.camel.ids.IdsProtocolComponent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * To exchange data with external Websocket servers using <a href="http://github.com/sonatype/async-http-client">Async Http Client</a>.
 */
@UriEndpoint(scheme = "idsclientplain,idsclient", extendsScheme = "ahc,ahc", title = "IDS Protocol",
        syntax = "idsclient:httpUri", consumerClass = WsConsumer.class, label = "websocket")
public class WsEndpoint extends AhcEndpoint {
    private static final transient Logger LOG = LoggerFactory.getLogger(WsEndpoint.class);

    private static List<IDSCPOutgoingConnection> outgoingConnections = new ArrayList<>();

	private final Set<WsConsumer> consumers = new HashSet<WsConsumer>();
    private final WsListener listener = new WsListener();
    private transient WebSocket websocket;

    @UriParam(label = "producer")
    private boolean useStreaming;
    @UriParam(label = "consumer")
    private boolean sendMessageOnError;
    @UriParam(label = "attestation", defaultValue = "0", description = "defines the remote attestation mode: 0=BASIC, 1=ALL, 2=ADVANCED, 3=ZERO. default value is 0=BASIC. (see api/attestation.proto for more details)")
    private Integer attestation = 0;
    @UriParam(label = "attestationMask", defaultValue = "0", description = "defines the upper boundary of PCR values tested in ADVANCED mode. i.e. attestationMask=5 means values PCR0, PCR1, PCR2, PCR3 and PCR4")
    private Integer attestationMask = 0;

    public WsEndpoint(String endpointUri, WsComponent component) {
        super(endpointUri, component, null);
    }

    @Override
    public WsComponent getComponent() {
        return (WsComponent) super.getComponent();
    }

    @Override
    public Producer createProducer() throws Exception {
        return new WsProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new WsConsumer(this, processor);
    }
    
    public static List<IDSCPOutgoingConnection> getOutgoingConnections() {
		return outgoingConnections;
	}

    WebSocket getWebSocket() throws Exception {
        synchronized (this) {
            // ensure we are connected
            reConnect();
        }
        return websocket;
    }

    void setWebSocket(WebSocket websocket) {
        this.websocket = websocket;
    }

    public boolean isUseStreaming() {
        return useStreaming;
    }

    /**
     * To enable streaming to send data as multiple text fragments.
     */
    public void setUseStreaming(boolean useStreaming) {
        this.useStreaming = useStreaming;
    }

    public boolean isSendMessageOnError() {
        return sendMessageOnError;
    }

    public void setAttestation(int type) {
        this.attestation = type;
    }

    public int getAttestation() {
        return attestation;
    }

    public void setAttestationMask(int type) {
        this.attestationMask = type;
    }

    public int getAttestationMask() {
        return attestationMask;
    }

    
    /**
     * Whether to send an message if the web-socket listener received an error.
     */
    public void setSendMessageOnError(boolean sendMessageOnError) {
        this.sendMessageOnError = sendMessageOnError;
    }

    @Override
    protected AsyncHttpClient createClient(AsyncHttpClientConfig config) {
        AsyncHttpClient client;
        if (config == null) {
            config = new DefaultAsyncHttpClientConfig.Builder().build();
            client = new DefaultAsyncHttpClient(config);
        } else {
            client = new DefaultAsyncHttpClient(config);
        }
        return client;
    }

    public void connect() throws Exception {
    	String uri = getHttpUri().toASCIIString();
    	if (uri.startsWith("idsclient:")) {
    		uri = uri.replaceFirst("idsclient:", "wss:");
    	} else if (uri.startsWith("idsclientplain:")) {
    		uri = uri.replaceFirst("idsclientplain:", "ws:");
    	}
    	
        LOG.debug("Connecting to {}", uri);
        BoundRequestBuilder reqBuilder = getClient().prepareGet(uri).addHeader("Sec-WebSocket-Protocol", "ids");
        
        LOG.debug("remote-attestation mode: {}", this.getAttestation());
        LOG.debug("remote-attestation mask: {}", this.getAttestationMask());
        
        // Execute IDS protocol immediately after connect
        IDSPListener idspListener = new IDSPListener(this.getAttestation(), this.getAttestationMask(), this.getSslContextParameters());
        websocket = reqBuilder.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(idspListener).build()).get();
        // wait for IDS protocol to finish 
        idspListener.semaphore().lockInterruptibly();
        boolean ratSuccess;
        try {
	        idspListener.isFinished().await();
        } finally {
        	ratSuccess = idspListener.isAttestationSuccessful();
        	//attestationResult = idspListener.getAttestationResult();
        	idspListener.semaphore().unlock();
        }
        
        LOG.debug("remote attestation was successful: " + ratSuccess);
        //get remote address, get upgrade headers, instantinate connectionmanagersertvice to register websocket
        // When IDS protocol has finished, hand over to normal web socket listener
        //This does not work! 
        websocket.addWebSocketListener(listener);
        websocket.removeWebSocketListener(idspListener);
        
        //Add Client Endpoint information to static List
        IDSCPOutgoingConnection ce = new IDSCPOutgoingConnection();
        ce.setAttestationResult(idspListener.getAttestationResult());
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
            websocket.close();
            websocket = null;
        }

        outgoingConnections.removeIf(ic-> ic.getEndpointKey().equals(this.getEndpointKey()));
        super.doStop();
    }

    void connect(WsConsumer wsConsumer) throws Exception {
        consumers.add(wsConsumer);
        reConnect();
    }

    void disconnect(WsConsumer wsConsumer) {
        consumers.remove(wsConsumer);
    }

    void reConnect() throws Exception {
        if (websocket == null || !websocket.isOpen()) {
            String uri = getHttpUri().toASCIIString();
            LOG.info("Reconnecting websocket: {}", uri);
            connect();
        }
    }

    class WsListener extends DefaultWebSocketListener {

        @Override
        public void onOpen(WebSocket websocket) {
            LOG.debug("Websocket opened");
        }

        @Override
        public void onClose(WebSocket websocket) {
            LOG.debug("websocket closed - reconnecting");
            try {
                reConnect();
            } catch (Exception e) {
                LOG.warn("Error re-connecting to websocket", e);
            }
        }

        @Override
        public void onError(Throwable t) {
            LOG.debug("websocket on error", t);
            if (isSendMessageOnError()) {
                for (WsConsumer consumer : consumers) {
                    consumer.sendMessage(t);
                }
            }
        }

        @Override
        public void onMessage(byte[] message) {
            LOG.debug("Received message --> {}", message);
            for (WsConsumer consumer : consumers) {
                consumer.sendMessage(message);
            }
        }

        @Override
        public void onMessage(String message) {
            LOG.debug("Received message --> {}", message);
            for (WsConsumer consumer : consumers) {
                consumer.sendMessage(message);
            }
        }

    }

}
