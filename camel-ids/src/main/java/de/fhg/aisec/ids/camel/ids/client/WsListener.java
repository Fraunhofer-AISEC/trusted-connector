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

import java.util.Set;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsListener implements WebSocketListener {
  private static final Logger LOG = LoggerFactory.getLogger(WsListener.class);

  private final Set<WsConsumer> consumers;
  private final WsEndpoint endpoint;

  public WsListener(Set<WsConsumer> consumers, WsEndpoint endpoint) {
    this.consumers = consumers;
    this.endpoint = endpoint;
  }

  @Override
  public void onOpen(WebSocket websocket) {
    LOG.debug("Websocket opened");
  }

  @Override
  public void onClose(WebSocket websocket, int code, String status) {
    LOG.debug("websocket closed - reconnecting");
    try {
      this.endpoint.reConnect();
    } catch (Exception e) {
      LOG.warn("Error re-connecting to websocket", e);
    }
  }

  @Override
  public void onError(Throwable t) {
    LOG.debug("websocket on error", t);
    if (endpoint.isSendMessageOnError()) {
      for (WsConsumer consumer : consumers) {
        consumer.sendMessage(t);
      }
    }
  }

  @Override
  public void onBinaryFrame(byte[] message, boolean finalFragment, int rsv) {
    LOG.debug("Received message --> {}", message);
    for (WsConsumer consumer : consumers) {
      consumer.sendMessage(message);
    }
  }

  @Override
  public void onTextFrame(String message, boolean finalFragment, int rsv) {
    LOG.debug("Received message --> {}", message);
    for (WsConsumer consumer : consumers) {
      consumer.sendMessage(message);
    }
  }
}
