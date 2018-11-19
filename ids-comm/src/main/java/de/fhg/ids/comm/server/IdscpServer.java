/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.ids.comm.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A standalone server implementation for the IDSCP protocol.
 *
 * <p>Simply call <code>start()</code> and use the registered <code>SocketListener</code> to
 * handling incoming WebSocket connections.
 *
 * <p>Make sure to check <code>handleAttestationResult()</code> and <code>getMetaData()</code> to
 * assess trustworthiness of the remote endpoint and the self description returned by it.
 */
public class IdscpServer {
  private static final Logger LOG = LoggerFactory.getLogger(IdscpServer.class);

  private ServerConfiguration config = new ServerConfiguration();
  private Server server;
  private SocketListener socketListener;

  public IdscpServer config(ServerConfiguration config) {
    this.config = config;
    return this;
  }

  public IdscpServer setSocketListener(SocketListener socketListener) {
    this.socketListener = socketListener;
    return this;
  }

  public IdscpServer start() {
    Server s = new Server(this.config.getPort());

    // Setup the basic application "context" for this application at "/"
    // This is also known as the handler tree (in jetty speak)
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    s.setHandler(context);

    // Add a websocket to a specific path spec
    ServletHolder holderEvents =
        new ServletHolder("ids", new ServerSocketServlet(this.config, this.socketListener));
    context.addServlet(holderEvents, "/");

    try {
      s.start();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    this.server = s;
    return this;
  }

  /**
   * Adds a ServletContextHandler to a (possibly running) server.
   *
   * @return
   */
  public IdscpServer addServlet(String basePath) {
    // TODO There should be one server per host/port, which is started at the first call to start().
    // This method should be removed and instead start() should register new basePaths, if necessary
    if (this.server == null) {
      throw new IllegalArgumentException("Wrong order: must call start() before addServlet()");
    }
    assert this.server != null;

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    this.server.setHandler(context);

    // Add a websocket to a specific path spec
    ServletHolder holderEvents =
        new ServletHolder("ids", new ServerSocketServlet(this.config, this.socketListener));
    context.addServlet(holderEvents, basePath);
    return this;
  }

  public Server getServer() {
    return this.server;
  }

  public boolean isRunning() {
    Server s = this.server;
    return s != null && s.isRunning();
  }
}
