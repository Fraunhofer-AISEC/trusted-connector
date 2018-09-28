/*-
 * ========================LICENSE_START=================================
 * ids-container-manager
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
package de.fhg.ids.cm.impl.trustx;

import jnr.unixsocket.UnixSocketChannel;

class ServerDataEvent {
  public TrustXMock server;
  public UnixSocketChannel socket;
  public byte[] data;

  public ServerDataEvent(TrustXMock server, UnixSocketChannel socket, byte[] data) {
    this.server = server;
    this.socket = socket;
    this.data = data;
  }
}
