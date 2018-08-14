/*-
 * ========================LICENSE_START=================================
 * camel-ids
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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
package de.fhg.camel.ids.server;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebsocketComponentServlet extends WebSocketServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(WebsocketComponentServlet.class);

  private final NodeSynchronization sync;
  private final Map<String, WebSocketFactory> socketFactories;
  private final String pathSpec;
  private final File tpmSocket;

  private WebsocketConsumer consumer;
  private static final ConcurrentMap<String, WebsocketConsumer> consumers =
      new ConcurrentHashMap<>();

  public WebsocketComponentServlet(
      NodeSynchronization sync,
      String pathSpec,
      Map<String, WebSocketFactory> socketFactories,
      File tpmSocket) {
    this.sync = sync;
    this.socketFactories = socketFactories;
    this.pathSpec = pathSpec;
    this.tpmSocket = tpmSocket;
  }

  public WebsocketConsumer getConsumer() {
    return consumer;
  }

  public void setConsumer(WebsocketConsumer consumer) {
    this.consumer = consumer;
  }

  public void connect(WebsocketConsumer consumer) {
    LOG.debug("Connecting consumer: {}", consumer);
    consumers.put(consumer.getPath(), consumer);
  }

  public void disconnect(WebsocketConsumer consumer) {
    LOG.debug("Disconnecting consumer: {}", consumer);
    consumers.remove(consumer.getPath());
  }

  public DefaultWebsocket doWebSocketConnect(ServletUpgradeRequest request, String protocol) {
    String protocolKey = protocol;

    if (protocol == null || !socketFactories.containsKey(protocol)) {
      LOG.debug(
          "No factory found for the socket protocol: {}, returning default implementation",
          protocol);
      protocolKey = "ids";
    }

    WebSocketFactory factory = socketFactories.get(protocolKey);
    return factory.newInstance(
        request,
        protocolKey,
        (consumer != null && consumer.getEndpoint() != null)
            ? WebsocketComponent.createPathSpec(consumer.getEndpoint().getResourceUri())
            : null,
        sync,

  consumer,

  tpmSocket);
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator(
          (req, resp) -> {
            String protocolKey = "ids";
            if (req.getSubProtocols().isEmpty() || req.getSubProtocols().contains(protocolKey)) {
              WebSocketFactory wsFactory = socketFactories.get(protocolKey);
              resp.setAcceptedSubProtocol(protocolKey);
              return wsFactory.newInstance(req, protocolKey, pathSpec, sync, consumer, tpmSocket);
            } else {
              LOG.error(
                  "WS subprotocols not supported: {}", String.join(",", req.getSubProtocols()));
              return null;
            }

        });
  }
}
