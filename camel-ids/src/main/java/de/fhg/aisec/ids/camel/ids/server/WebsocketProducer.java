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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;
import org.asynchttpclient.netty.handler.StreamedResponsePublisher;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WebsocketProducer extends DefaultProducer implements WebsocketProducerConsumer {

  private WebsocketStore store;
  private final WebsocketEndpoint endpoint;

  public WebsocketProducer(WebsocketEndpoint endpoint) {
    super(endpoint);
    this.endpoint = endpoint;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Message in = exchange.getIn();
    Object message = in.getMandatoryBody();
    if (!(message == null || message instanceof String || message instanceof byte[])) {
      message = in.getMandatoryBody(String.class);
    }

    // look for connection key and get Websocket
    String connectionKey = in.getHeader(WebsocketConstants.CONNECTION_KEY, String.class);
    if (connectionKey != null) {
      String pathSpec = "";
      if (endpoint.getResourceUri() != null) {
        pathSpec = WebsocketComponent.createPathSpec(endpoint.getResourceUri());
      }
      DefaultWebsocket websocket = store.get(connectionKey + pathSpec);
      log.debug("Sending to connection key {} -> {}", connectionKey, message);
      Future<Void> future = sendMessage(websocket, message);
      StreamedResponsePublisher sp;
      if (future != null) {
        int timeout = endpoint.getSendTimeout();
        future.get(timeout, TimeUnit.MILLISECONDS);
        if (!future.isCancelled() && !future.isDone()) {
          throw new WebsocketSendException(
              "Failed to send message to the connection within " + timeout + " millis.", exchange);
        }
      }
    } else {
      throw new WebsocketSendException(
          "Failed to send message to single connection; connection key not set.", exchange);
    }
  }

  public WebsocketEndpoint getEndpoint() {
    return endpoint;
  }

  @Override
  public void doStart() throws Exception {
    super.doStart();
    endpoint.connect(this);
  }

  @Override
  public void doStop() throws Exception {
    endpoint.disconnect(this);
    super.doStop();
  }

  Future<Void> sendMessage(DefaultWebsocket websocket, Object message) {
    Future<Void> future = null;
    // in case there is web socket and socket connection is open - send message
    if (websocket != null && websocket.getSession().isOpen()) {
      log.trace("Sending to websocket {} -> {}", websocket.getConnectionKey(), message);
      if (message instanceof String) {
        future = websocket.getSession().getRemote().sendStringByFuture((String) message);
      } else if (message instanceof byte[]) {
        ByteBuffer buf = ByteBuffer.wrap((byte[]) message);
        future = websocket.getSession().getRemote().sendBytesByFuture(buf);
      }
    }
    return future;
  }

  // Store is set/unset upon connect/disconnect of the producer
  public void setStore(WebsocketStore store) {
    this.store = store;
  }

  /** Called when a sleep is interrupted; allows derived classes to handle this case differently */
  protected void handleSleepInterruptedException(InterruptedException e, Exchange exchange)
      throws InterruptedException {
    if (log.isDebugEnabled()) {
      log.debug("Sleep interrupted, are we stopping? {}", isStopping() || isStopped());
    }
    Thread.currentThread().interrupt();
    throw e;
  }
}
