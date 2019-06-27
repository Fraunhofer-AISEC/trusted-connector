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
package de.fhg.aisec.ids.cm.impl.trustx;

import jnr.unixsocket.UnixSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TrustXMockHandler implements Runnable {
  private Logger LOG = LoggerFactory.getLogger(TrustXMockHandler.class);
  private List<ServerDataEvent> queue = new LinkedList<>();

  @Override
  public void run() {
    ServerDataEvent dataEvent;

    while (true) {
      // Wait for data to become available
      synchronized (queue) {
        while (queue.isEmpty()) {
          try {
            queue.wait();
          } catch (InterruptedException e) {
          }
        }
        dataEvent = (ServerDataEvent) queue.remove(0);
      }

      // Print
      System.out.println(new String(dataEvent.data));
      dataEvent.server.send(dataEvent.socket, dataEvent.data);
    }
  }

  public void handleResponse(TrustXMock server, UnixSocketChannel socket, byte[] data, int count) {
    byte[] dataCopy = new byte[count];
    System.arraycopy(data, 0, dataCopy, 0, count);
    synchronized (queue) {
      queue.add(new ServerDataEvent(server, socket, dataCopy));
      queue.notify();
    }
  }
}
