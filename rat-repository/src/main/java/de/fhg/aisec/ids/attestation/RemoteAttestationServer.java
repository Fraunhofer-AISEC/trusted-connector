/*-
 * ========================LICENSE_START=================================
 * rat-repository
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
package de.fhg.aisec.ids.attestation;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class RemoteAttestationServer {
  private Server server;
  private Database database;
  private int PORT = 0;
  private String host;
  private String path;
  private URI uri;
  private Logger LOG = LoggerFactory.getLogger(Database.class);

  public RemoteAttestationServer(String host, String path, int port) {
    this.database = new Database();
    this.host = host;
    this.path = path;
    this.PORT = port;
    try {
      this.uri = new URI(String.format("https://%s:%d/%s", this.host, this.PORT, this.path));
      LOG.debug("Remote Attestation Repository starting on : " + this.uri.toURL().toString());
      server = new Server();

      // HTTP Configuration
      HttpConfiguration http_config = new HttpConfiguration();
      http_config.setSecureScheme("https");
      http_config.setSecurePort(this.PORT);

      // === jetty-https.xml ===
      // SSL Context Factory
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(
          Thread.currentThread()
              .getContextClassLoader()
              .getResource("repository-keystore.jks")
              .toString());
      sslContextFactory.setKeyStorePassword("OBF:1v2j1uum1xtv1zej1zer1xtn1uvk1v1v");
      sslContextFactory.setKeyManagerPassword("OBF:1v2j1uum1xtv1zej1zer1xtn1uvk1v1v");
      sslContextFactory.setTrustStorePath(
          Thread.currentThread()
              .getContextClassLoader()
              .getResource("repository-truststore.jks")
              .toString());
      sslContextFactory.setTrustStorePassword("OBF:1v2j1uum1xtv1zej1zer1xtn1uvk1v1v");
      sslContextFactory.setExcludeCipherSuites(
          "SSL_RSA_WITH_DES_CBC_SHA",
          "SSL_DHE_RSA_WITH_DES_CBC_SHA",
          "SSL_DHE_DSS_WITH_DES_CBC_SHA",
          "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
          "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
          "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
          "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

      // SSL HTTP Configuration
      HttpConfiguration https_config = new HttpConfiguration(http_config);
      https_config.addCustomizer(new SecureRequestCustomizer());

      // SSL Connector
      ServerConnector sslConnector =
          new ServerConnector(
              server,
              new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
              new HttpConnectionFactory(https_config));
      sslConnector.setPort(this.PORT);
      server.addConnector(sslConnector);
      ServletContextHandler handler = new ServletContextHandler();
      handler.setContextPath("");
      handler.addServlet(new ServletHolder(new ServletContainer(resourceConfig())), "/*");
      server.setHandler(handler);
    } catch (URISyntaxException e) {
      LOG.debug("could not format URI !");
      e.printStackTrace();
    } catch (MalformedURLException e) {
      LOG.debug("could not format URI !");
      e.printStackTrace();
    }
  }

  public void start() {
    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException("Could not start the server", e);
    }
  }

  private ResourceConfig resourceConfig() {
    return new ResourceConfig().register(ProtobufProvider.class).register(new REST(this.database));
  }

  public void stop() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException("Could not stop the server", e);
    }
  }

  public void join() {
    try {
      server.join();
    } catch (InterruptedException e) {
      throw new RuntimeException("Could not join the thread", e);
    }
  }

  public URI getURI() {
    return this.uri;
  }

  public Database getDatabase() {
    return this.database;
  }

  public void destroy() {
    server.destroy();
  }
}
