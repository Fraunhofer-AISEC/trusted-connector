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
package de.fhg.aisec.ids.comm.client;

import de.fhg.aisec.ids.api.conm.RatResult;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.asynchttpclient.Dsl.asyncHttpClient;

/**
 * A standalone client implementation for the IDSCP protocol.
 *
 * <p>Simply call <code>connect()</code> and use the returned WebSocket object for bidirectional
 * text/binary web socket communication with the remote endpoint.
 *
 * <p>Make sure to check <code>handleAttestationResult()</code> and <code>getMetaData()</code> to
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
   * @throws NoSuchAlgorithmException Use <code>connect(String host, int port)</code> instead.
   */
  @Deprecated
  public WebSocket connect(URI uri) throws InterruptedException, ExecutionException,
      KeyManagementException, NoSuchAlgorithmException {
    return connect(uri.getHost(), uri.getPort());
  }

  /**
   * Connects to a remote endpoint, executes the IDSCP handshake and returns the ready-to-use
   * WebSocket object.
   */
  public WebSocket connect(@NonNull String host, int port)
      throws InterruptedException, ExecutionException, KeyManagementException,
      NoSuchAlgorithmException {
    final Builder builder = new Builder();
    if (!this.config.getSha256CertificateHashes().isEmpty()) {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new X509TrustManager[]{new X509TrustManager() {
        @Override
        public void checkServerTrusted(X509Certificate[] certs, String str)
            throws CertificateException {
          try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = digest.digest(certs[0].getEncoded());
            if (config.getSha256CertificateHashes().stream()
                .noneMatch(hash -> Arrays.equals(digestBytes, hash))) {
              throw new CertificateException("Did not find pinned SHA256 certificate hash: "
                  + DatatypeConverter.printHexBinary(digestBytes));
            }
          } catch (Exception x) {
            throw new CertificateException("Error during hash calculation", x);
          }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String str)
            throws CertificateException {
          throw new CertificateException("Must not be called by client implementation!");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }
      }}, null);
      SslContext sslContext = new JdkSslContext(ctx, true, ClientAuth.NONE);
      builder.setSslContext(sslContext);
    }
    final AsyncHttpClient c = asyncHttpClient(builder.build());

    // Connect to web socket
    IdspClientSocket wsListener = new IdspClientSocket(this.config);
    
    WebSocket ws =
        c.prepareGet("wss://" + host + ":" + port + "/" + this.config.getEndpoint())
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
   */
  @NonNull
  public IdscpClient config(@NonNull ClientConfiguration config) {
    this.config = config;
    return this;
  }

  /**
   * Returns null if attestation has not yet finished, or status code of remote attestation
   * otherwise.
   */
  @Nullable
  public RatResult getAttestationResult() {
    return this.attestationResult;
  }

  /**
   * Returns meta data about the remote endpoint or <code>null</code> if no meta data has been
   * exchanged.
   */
  @Nullable
  public String getMetaData() {
    return this.metaData;
  }
}
