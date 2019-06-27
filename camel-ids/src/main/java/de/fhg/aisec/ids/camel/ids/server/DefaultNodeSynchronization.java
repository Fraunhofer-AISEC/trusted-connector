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
package de.fhg.aisec.ids.camel.ids.server;

public class DefaultNodeSynchronization implements NodeSynchronization {

  private final WebsocketStore memoryStore;

  public DefaultNodeSynchronization(WebsocketStore memoryStore) {
    this.memoryStore = memoryStore;
  }

  @Override
  public void addSocket(DefaultWebsocket socket) {
    memoryStore.add(socket);
  }

  @Override
  public void removeSocket(String id) {
    memoryStore.remove(id);
  }

  @Override
  public void removeSocket(DefaultWebsocket socket) {
    memoryStore.remove(socket);
  }
}
