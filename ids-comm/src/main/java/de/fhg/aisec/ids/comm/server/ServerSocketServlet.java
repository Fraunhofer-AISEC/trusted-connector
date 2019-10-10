/*-
 * ========================LICENSE_START=================================
 * ids-comm
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
package de.fhg.aisec.ids.comm.server;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class ServerSocketServlet extends WebSocketServlet {
  private static final long serialVersionUID = -3504454673920877370L;
  private final ServerConfiguration config;
  private SocketListener socketListener;

  public ServerSocketServlet(ServerConfiguration config, SocketListener socketListener) {
    this.config = config;
    this.socketListener = socketListener;
  }

  @Override
  public void configure(WebSocketServletFactory factory) {
    factory.setCreator(new IdscpWebSocketCreator(this.config, this.socketListener));
  }
}
