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

import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.fhg.aisec.ids.api.conm.RatResult;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;

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
   * @param uri
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws NoSuchAlgorithmException 
   * 
   * Use <code>connect(String host, int port)</code> instead.
 * @throws KeyManagementException 
   */
  @Deprecated
  public WebSocket connect(URI uri) throws InterruptedException, ExecutionException, NoSuchAlgorithmException, KeyManagementException {
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
 * @throws NoSuchAlgorithmException 
 * @throws KeyManagementException 
   */
  public WebSocket connect(@NonNull String host, int port) throws InterruptedException, ExecutionException, NoSuchAlgorithmException, KeyManagementException {
	AsyncHttpClient c = null;    
	if (this.config.isDisableServerVerification()) {
		System.err.println("TLS Server verification has been switched off! TLS connections are not secure. If this message appears in production, your data is at risk! Switch on server verification again and make sure to maintain a proper truststore for trusted server certificates!");
		SSLContext ctx = SSLContext.getInstance("TLSv1.2");
		ctx.init(null, new X509TrustManager[] { new X509TrustManager() {
			@Override
			public void checkServerTrusted(X509Certificate[] certs, String str) throws CertificateException {	}
			@Override
			public void checkClientTrusted(X509Certificate[] certs, String str) throws CertificateException {	}
			@Override
			public X509Certificate[] getAcceptedIssuers() { return null; }
		}}, null);
	    SslContext sslContext = new JdkSslContext(ctx, true, ClientAuth.NONE);
		Builder builder = new Builder();
		builder.setDisableHttpsEndpointIdentificationAlgorithm(true);
		builder.setUseInsecureTrustManager(true);
		builder.setSslContext(sslContext);
		c = asyncHttpClient(builder);
	  } else {
		  c = asyncHttpClient();
	  }
	assert c != null;

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
   *
   * @param config
   * @return
   */
  @NonNull
  public IdscpClient config(@Nullable ClientConfiguration config) {
    if (config != null) {
    	this.config = config;
    }
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
   *
   * @return
   */
  @Nullable
  public String getMetaData() {
    return this.metaData;
  }
}
