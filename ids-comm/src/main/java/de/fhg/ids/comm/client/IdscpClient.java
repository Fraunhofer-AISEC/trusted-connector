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
package de.fhg.ids.comm.client;

import de.fhg.aisec.ids.api.conm.RatResult;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import static org.asynchttpclient.Dsl.asyncHttpClient;

/**
 * A standalone client implementation for the IDSCP protocol.
 *
 * <p>Simply call <code>connect()</code> and use the returned WebSocket object for bidirectional
 * text/binary web socket communication with the remote endpoint.
 *
 * <p>Make sure to check <code>getAttestationResult()</code> and <code>getMetaData()</code> to
 * assess trustworthiness of the remote endpoint and the self description returned by it.
 */
public class IdscpClient {

  private ClientConfiguration config = new ClientConfiguration();
  private RatResult attestationResult = null;
  private String metaData = null;

  /**
   * Connects to a remote endpoint, executes the IDSCP handshake and returns the ready-to-use
   * WebSocket object.
   *
   * @param uri
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public WebSocket connect(URI uri) throws InterruptedException, ExecutionException {
    return connect(uri.getHost(), uri.getPort());
  }

  /**
   * Connects to a remote endpoint, executes the IDSCP handshake and returns the ready-to-use
   * WebSocket object.
   *
   * @param host
   * @param port
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public WebSocket connect(String host, int port) throws InterruptedException, ExecutionException {
    AsyncHttpClient c = asyncHttpClient();

    // Connect to web socket
    IdspClientSocket wsListener = new IdspClientSocket(this.config);
    WebSocket ws =
        c.prepareGet("ws://" + host + ":" + port + "/")
            .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(wsListener).build())
            .get();

    // Block until ISCP has finished
    wsListener.semaphore().lockInterruptibly();
    try {
      while (!wsListener.isTerminated()) {
        wsListener.idscpInProgressCondition().await();
      }
    } finally {
      this.attestationResult = wsListener.getAttestationResult();
      this.metaData = wsListener.getMetaResult();
      wsListener.semaphore().unlock();
    }
    return ws;
  }

  /**
   * Sets the configuration of this client.
   *
   * <p>Use this method in a fluent API style before calling <code>connect()</code>:
   *
   * <pre>
   * new IdscpClient().config(config).connect(url);
   * </pre>
   *
   * @param config
   * @return
   */
  public IdscpClient config(ClientConfiguration config) {
    this.config = config;
    return this;
  }

  /**
   * Returns null if attestation has not yet finished, or status code of remote attestation
   * otherwise.
   */
  public RatResult getAttestationResult() {
    return this.attestationResult;
  }

  /**
   * Returns meta data about the remote endpoint or <code>null</code> if no meta data has been
   * exchanged.
   *
   * @return
   */
  public String getMetaData() {
    return this.metaData;
  }
}
