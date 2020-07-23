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

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WebSocketComponentServlet extends WebSocketServlet {
  public static final int MAX_MESSAGE_SIZE = 100 * 1024 * 1024;

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(WebSocketComponentServlet.class);

  private final NodeSynchronization sync;
  private final Map<String, WebSocketFactory> socketFactories;
  private final String pathSpec;

  private WebSocketConsumer consumer;

  public WebSocketComponentServlet(
      NodeSynchronization sync, String pathSpec, Map<String, WebSocketFactory> socketFactories) {
    this.sync = sync;
    this.socketFactories = socketFactories;
    this.pathSpec = pathSpec;
  }

  public WebSocketConsumer getConsumer() {
    return consumer;
  }

  public void setConsumer(WebSocketConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.getPolicy().setMaxBinaryMessageSize(MAX_MESSAGE_SIZE);
    factory.getPolicy().setMaxTextMessageSize(MAX_MESSAGE_SIZE);
    factory.setCreator(
        (req, resp) -> {
          String protocolKey = "ids";
          if (req.getSubProtocols().isEmpty() || req.getSubProtocols().contains(protocolKey)) {
            WebSocketFactory wsFactory = socketFactories.get(protocolKey);
            resp.setAcceptedSubProtocol(protocolKey);
            return wsFactory.newInstance(req, protocolKey, pathSpec, sync, consumer);
          } else {
            LOG.error("WS subprotocols not supported: {}", String.join(",", req.getSubProtocols()));
            return null;
          }
        });
  }
}
