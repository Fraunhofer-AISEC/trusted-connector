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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class ConnectorRef {
  Server server;
  ServerConnector connector;
  WebsocketComponentServlet servlet;
  MemoryWebsocketStore memoryStore;
  int refCount;

  ConnectorRef(
      Server server,
      ServerConnector connector,
      WebsocketComponentServlet servlet,
      MemoryWebsocketStore memoryStore) {
    this.server = server;
    this.connector = connector;
    this.servlet = servlet;
    this.memoryStore = memoryStore;
    increment();
  }

  public int increment() {
    return ++refCount;
  }

  public int decrement() {
    return --refCount;
  }

  public int getRefCount() {
    return refCount;
  }

  public Server getServer() {
    return this.server;
  }

  public ServerConnector getConnector() {
    return this.connector;
  }

  public WebsocketComponentServlet getServlet() {
    return this.servlet;
  }

  public MemoryWebsocketStore getMemoryStore() {
    return this.memoryStore;
  }
}
