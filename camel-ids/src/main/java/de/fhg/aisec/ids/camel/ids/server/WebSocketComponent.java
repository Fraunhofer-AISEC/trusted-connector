/*-
 * ========================LICENSE_START=================================
 * camel-ids
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
package de.fhg.aisec.ids.camel.ids.server;

import de.fhg.aisec.ids.camel.ids.ProxyX509TrustManager;
import de.fhg.aisec.ids.camel.ids.WebSocketConstants;
import de.fhg.aisec.ids.comm.CertificatePair;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.spi.Metadata;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketComponent extends DefaultComponent implements SSLContextParametersAware {
  protected static final Logger LOG = LoggerFactory.getLogger(WebSocketComponent.class);
  protected static final Map<String, ConnectorRef> CONNECTORS = new HashMap<>();

  protected Map<String, WebSocketFactory> socketFactories;
  private final CertificatePair certificatePair = new CertificatePair();

  @Metadata(label = "security")
  protected SSLContextParameters sslContextParameters;

  @Metadata(label = "security", defaultValue = "false")
  protected boolean useGlobalSslContextParameters;

  @Metadata(label = "advanced")
  protected ThreadPool threadPool;

  @Metadata(defaultValue = WebSocketConstants.DEFAULT_PORT)
  protected Integer port = Integer.parseInt(WebSocketConstants.DEFAULT_PORT);

  @Metadata(label = "advanced")
  protected Integer minThreads;

  @Metadata(label = "advanced")
  protected Integer maxThreads;

  @Metadata(defaultValue = WebSocketConstants.DEFAULT_HOST)
  protected String host = WebSocketConstants.DEFAULT_HOST;

  /**
   * Map for storing servlets. {@link WebSocketComponentServlet} is identified by pathSpec {@link
   * String}.
   */
  private final Map<String, WebSocketComponentServlet> servlets = new HashMap<>();

  public static class ConnectorRef {
    Server server;
    ServerConnector connector;
    WebSocketComponentServlet servlet;
    MemoryWebSocketStore memoryStore;
    AtomicInteger refCount = new AtomicInteger(1);

    ConnectorRef(
        Server server,
        ServerConnector connector,
        WebSocketComponentServlet servlet,
        MemoryWebSocketStore memoryStore) {
      this.server = server;
      this.connector = connector;
      this.servlet = servlet;
      this.memoryStore = memoryStore;
    }

    public void increment() {
      refCount.incrementAndGet();
    }

    public int decrement() {
      return refCount.decrementAndGet();
    }

    public int getRefCount() {
      return refCount.get();
    }

    public MemoryWebSocketStore getMemoryStore() {
      return memoryStore;
    }

    public WebSocketComponentServlet getServlet() {
      return servlet;
    }

    public ServerConnector getConnector() {
      return connector;
    }
  }

  public WebSocketComponent() {
    this.setUseGlobalSslContextParameters(true);
    // this will automatically set up the ids handler factory
    this.setSocketFactories(new HashMap<>());
  }

  /** Connects the URL specified on the endpoint to the specified processor. */
  public void connect(WebSocketProducerConsumer prodcon) throws Exception {
    WebSocketEndpoint endpoint = prodcon.getEndpoint();

    String connectorKey = getConnectorKey(endpoint);

    synchronized (CONNECTORS) {
      ConnectorRef connectorRef = CONNECTORS.get(connectorKey);
      if (connectorRef == null) {
        // Create Server and add connector
        Server server = createServer();

        ServerConnector connector = getSocketConnector(server, endpoint.getSslContextParameters());

        if (endpoint.getPort() != null) {
          connector.setPort(endpoint.getPort());
        } else {
          connector.setPort(port);
        }

        if (endpoint.getHost() != null) {
          connector.setHost(endpoint.getHost());
        } else {
          connector.setHost(host);
        }

        server.addConnector(connector);

        LOG.trace("Jetty Connector added: {}", connector.getName());

        // Create ServletContextHandler
        ServletContextHandler context = createContext(server, connector, endpoint.getHandlers());
        // setup the WebSocketComponentServlet initial parameters
        setWebSocketComponentServletInitialParameter(context, endpoint);
        server.setHandler(context);

        // Apply CORS (http://www.w3.org/TR/cors/)
        applyCrossOriginFiltering(endpoint, context);

        MemoryWebSocketStore memoryStore = new MemoryWebSocketStore();

        // Don't provide a Servlet object as Producer/Consumer will create them later on
        connectorRef = new ConnectorRef(server, connector, null, memoryStore);

        // must enable session before we start
        if (endpoint.isSessionSupport()) {
          enableSessionSupport(connectorRef.server, connectorKey);
        }
        LOG.info("Jetty Server starting on host: {}:{}", connector.getHost(), connector.getPort());
        connectorRef.memoryStore.start();
        connectorRef.server.start();

        CONNECTORS.put(connectorKey, connectorRef);
      } else {
        connectorRef.increment();
      }

      // check the session support
      if (endpoint.isSessionSupport()) {
        enableSessionSupport(connectorRef.server, connectorKey);
      }

      NodeSynchronization sync = new DefaultNodeSynchronization(connectorRef.memoryStore);
      WebSocketComponentServlet servlet = addServlet(sync, prodcon, endpoint.getResourceUri());
      if (prodcon instanceof WebSocketConsumer) {
        WebSocketConsumer consumer = (WebSocketConsumer) prodcon;
        if (servlet.getConsumer() == null) {
          servlet.setConsumer(consumer);
        }
      }
      if (prodcon instanceof WebSocketProducer) {
        WebSocketProducer producer = (WebSocketProducer) prodcon;
        producer.setStore(connectorRef.memoryStore);
      }
    }
  }

  /** Disconnects the URL specified on the endpoint from the specified processor. */
  public void disconnect(WebSocketProducerConsumer prodcon) throws Exception {
    // If the connector is not needed anymore then stop it
    WebSocketEndpoint endpoint = prodcon.getEndpoint();
    String connectorKey = getConnectorKey(endpoint);

    synchronized (CONNECTORS) {
      ConnectorRef connectorRef = CONNECTORS.get(connectorKey);
      if (connectorRef != null) {
        if (connectorRef.decrement() == 0) {
          LOG.info(
              "Stopping Jetty Server as the last connector is disconnecting: {}: {}",
              connectorRef.connector.getHost(),
              connectorRef.connector.getPort());
          servlets.remove(createPathSpec(endpoint.getResourceUri()));
          connectorRef.server.removeConnector(connectorRef.connector);
          if (connectorRef.connector != null) {
            // static server may not have set a connector
            connectorRef.connector.stop();
          }
          connectorRef.server.stop();
          connectorRef.memoryStore.stop();
          CONNECTORS.remove(connectorKey);
          // Camel controls the lifecycle of these entities so remove the
          // registered MBeans when Camel is done with the managed objects.
        }
        if (prodcon instanceof WebSocketProducer) {
          ((WebSocketProducer) prodcon).setStore(null);
        }
      }
    }
  }

  @Override
  protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
      throws Exception {
    SSLContextParameters sslContextParameters =
        resolveAndRemoveReferenceParameter(
            parameters, "sslContextParameters", SSLContextParameters.class);

    int port = extractPortNumber(remaining);
    String host = extractHostName(remaining);

    WebSocketEndpoint endpoint = new WebSocketEndpoint(this, uri, remaining);

    // prefer to use endpoint configured over component configured
    if (sslContextParameters == null) {
      // fallback to component configured
      sslContextParameters = getSslContextParameters();
    }
    if (sslContextParameters == null) {
      sslContextParameters = retrieveGlobalSslContextParameters();
    }

    endpoint.setSslContextParameters(sslContextParameters);
    endpoint.setPort(port);
    endpoint.setHost(host);

    setProperties(endpoint, parameters);
    return endpoint;
  }

  protected void setWebSocketComponentServletInitialParameter(
      ServletContextHandler context, WebSocketEndpoint endpoint) {
    if (endpoint.getBufferSize() != null) {
      context.setInitParameter("bufferSize", endpoint.getBufferSize().toString());
    }
    if (endpoint.getMaxIdleTime() != null) {
      context.setInitParameter("maxIdleTime", endpoint.getMaxIdleTime().toString());
    }
    if (endpoint.getMaxTextMessageSize() != null) {
      context.setInitParameter("maxTextMessageSize", endpoint.getMaxTextMessageSize().toString());
    }
    if (endpoint.getMaxBinaryMessageSize() != null) {
      context.setInitParameter(
          "maxBinaryMessageSize", endpoint.getMaxBinaryMessageSize().toString());
    }
    if (endpoint.getMinVersion() != null) {
      context.setInitParameter("minVersion", endpoint.getMinVersion().toString());
    }
  }

  protected Server createServer() {
    Server server = null;
    if (minThreads == null && maxThreads == null && getThreadPool() == null) {
      minThreads = 1;
      // 1+selectors+acceptors
      maxThreads = 2 * (1 + Runtime.getRuntime().availableProcessors() * 2);
    }
    // configure thread pool if min/max given
    if (minThreads != null || maxThreads != null) {
      if (getThreadPool() != null) {
        throw new IllegalArgumentException(
            "You cannot configure both minThreads/maxThreads "
                + "and a custom threadPool on JettyHttpComponent: "
                + this);
      }
      QueuedThreadPool qtp = new QueuedThreadPool();
      if (minThreads != null) {
        qtp.setMinThreads(minThreads);
      }
      if (maxThreads != null) {
        qtp.setMaxThreads(maxThreads);
      }
      // let the thread names indicate they are from the server
      qtp.setName("CamelJettyWebSocketServer");
      try {
        qtp.start();
      } catch (Exception e) {
        throw new RuntimeCamelException(
            "Error starting JettyWebSocketServer thread pool: " + qtp, e);
      }
      server = new Server(qtp);
      ContextHandlerCollection collection = new ContextHandlerCollection();
      server.setHandler(collection);
    }

    if (getThreadPool() != null) {
      server = new Server(getThreadPool());
      ContextHandlerCollection collection = new ContextHandlerCollection();
      server.setHandler(collection);
    }

    return server;
  }

  protected WebSocketComponentServlet addServlet(
          NodeSynchronization sync, WebSocketProducerConsumer prodcon, String resourceUri)
      throws Exception {

    // Get Connector from one of the Jetty Instances to add WebSocket Servlet
    WebSocketEndpoint endpoint = prodcon.getEndpoint();
    String key = getConnectorKey(endpoint);
    ConnectorRef connectorRef = getConnectors().get(key);

    WebSocketComponentServlet servlet;

    if (connectorRef != null) {
      String pathSpec = createPathSpec(resourceUri);
      servlet = servlets.get(pathSpec);
      if (servlet == null) {
        // Retrieve Context
        ServletContextHandler context = (ServletContextHandler) connectorRef.server.getHandler();
        servlet = createServlet(sync, pathSpec, servlets, context);
        connectorRef.servlet = servlet;
        LOG.debug(
            "WebSocket servlet added for the following path: {}, to the Jetty Server: {}",
            pathSpec,
            key);
      }

      return servlet;
    } else {
      throw new Exception("Jetty instance has not been retrieved for : " + key);
    }
  }

  protected WebSocketComponentServlet createServlet(
      NodeSynchronization sync,
      String pathSpec,
      Map<String, WebSocketComponentServlet> servlets,
      ServletContextHandler handler) {
    WebSocketComponentServlet servlet =
        new WebSocketComponentServlet(sync, pathSpec, getSocketFactories());
    servlets.put(pathSpec, servlet);
    ServletHolder servletHolder = new ServletHolder(servlet);
    servletHolder.getInitParameters().putAll(handler.getInitParams());
    // Jetty 9 parameter bufferSize is now inputBufferSize
    servletHolder.setInitParameter("inputBufferSize", handler.getInitParameter("bufferSize"));
    handler.addServlet(servletHolder, pathSpec);
    return servlet;
  }

  protected ServletContextHandler createContext(
      Server server, Connector connector, List<Handler> handlers) {
    ServletContextHandler context =
        new ServletContextHandler(
            server, "/", ServletContextHandler.NO_SECURITY | ServletContextHandler.NO_SESSIONS);
    server.addConnector(connector);

    if (handlers != null && !handlers.isEmpty()) {
      for (Handler handler : handlers) {
        if (handler instanceof HandlerWrapper) {
          ((HandlerWrapper) handler).setHandler(server.getHandler());
          server.setHandler(handler);
        } else {
          HandlerCollection handlerCollection = new HandlerCollection();
          handlerCollection.addHandler(server.getHandler());
          handlerCollection.addHandler(handler);
          server.setHandler(handlerCollection);
        }
      }
    }

    return context;
  }

  private void enableSessionSupport(Server server, String connectorKey) {
    ServletContextHandler context = server.getChildHandlerByClass(ServletContextHandler.class);
    if (context.getSessionHandler() == null) {
      SessionHandler sessionHandler = new SessionHandler();
      if (context.isStarted()) {
        throw new IllegalStateException(
            "Server has already been started. "
                + "Cannot enabled sessionSupport on "
                + connectorKey);
      } else {
        context.setSessionHandler(sessionHandler);
      }
    }
  }

  private ServerConnector getSocketConnector(
      Server server, SSLContextParameters sslContextParameters)
      throws GeneralSecurityException, IOException {
    if (sslContextParameters == null) {
      sslContextParameters = retrieveGlobalSslContextParameters();
    }
    if (sslContextParameters != null) {
      try {
        ProxyX509TrustManager.bindCertificatePair(sslContextParameters, true, certificatePair);
      } catch (GeneralSecurityException | IOException e) {
        LOG.error("Failed to patch TrustManager for WebSocketComponent", e);
      }
      SslContextFactory sslContextFactory = new SslContextFactory.Server();
      sslContextFactory.setSslContext(sslContextParameters.createSSLContext(getCamelContext()));
      return new ServerConnector(server, sslContextFactory);
    } else {
      return new ServerConnector(server);
    }
  }

  public static String createPathSpec(String remaining) {
    int index = remaining.indexOf('/');
    if (index != -1) {
      return remaining.substring(index);
    } else {
      return "/" + remaining;
    }
  }

  private int extractPortNumber(String remaining) {
    int index1 = remaining.indexOf(':');
    int index2 = remaining.indexOf('/');

    if ((index1 != -1) && (index2 != -1)) {
      String result = remaining.substring(index1 + 1, index2);
      return Integer.parseInt(result);
    } else {
      return port;
    }
  }

  private String extractHostName(String remaining) {
    int index = remaining.indexOf(':');
    if (index != -1) {
      return remaining.substring(0, index);
    } else {
      return host;
    }
  }

  private static String getConnectorKey(WebSocketEndpoint endpoint) {
    String host = endpoint.getHost();
    if (isLocalAddress(endpoint.getHost())) {
      // replace host name with `local` to be sure, that we will have one ConnectorRef for hosts like localhost or
      // 127.0.0.1 or 0.0.0.0
      host = "local";
    }
    return endpoint.getProtocol() + ":" + host + ":" + endpoint.getPort();
  }

  private static boolean isLocalAddress(String host) {
    InetAddress address;
    InetAddress localHost;
    try {
      address = InetAddress.getByName(host);
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      return false;
    }
    return address.isAnyLocalAddress() || address.isLoopbackAddress() || localHost.equals(address);
  }

  private void applyCrossOriginFiltering(
          WebSocketEndpoint endpoint, ServletContextHandler context) {
    if (endpoint.isCrossOriginFilterOn()) {
      FilterHolder filterHolder = new FilterHolder();
      CrossOriginFilter filter = new CrossOriginFilter();
      filterHolder.setFilter(filter);
      filterHolder.setInitParameter("allowedOrigins", endpoint.getAllowedOrigins());
      context.addFilter(
          filterHolder, endpoint.getFilterPath(), EnumSet.allOf(DispatcherType.class));
    }
  }

  public ThreadPool getThreadPool() {
    return threadPool;
  }

  public SSLContextParameters getSslContextParameters() {
    return sslContextParameters;
  }

  @Override
  public boolean isUseGlobalSslContextParameters() {
    return this.useGlobalSslContextParameters;
  }

  /** Enable usage of global SSL context parameters. */
  @Override
  public void setUseGlobalSslContextParameters(boolean useGlobalSslContextParameters) {
    this.useGlobalSslContextParameters = useGlobalSslContextParameters;
  }

  public Map<String, WebSocketFactory> getSocketFactories() {
    return socketFactories;
  }

  /**
   * To configure a map which contains custom WebSocketFactory for sub protocols. The key in the map
   * is the sub protocol.
   *
   * <p>The <tt>default</tt> key is reserved for the default implementation.
   */
  public void setSocketFactories(Map<String, WebSocketFactory> socketFactories) {
    this.socketFactories = socketFactories;

    if (!this.socketFactories.containsKey("ids")) {
      this.socketFactories.put("ids", new DefaultWebSocketFactory(certificatePair));
    }
  }

  public static Map<String, ConnectorRef> getConnectors() {
    return CONNECTORS;
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
  }

  @Override
  public void doStop() throws Exception {
    super.doStop();
    synchronized (CONNECTORS) {
      var iterator = CONNECTORS.entrySet().iterator();
      while (iterator.hasNext()) {
        var connectorRef = iterator.next().getValue();
        if (connectorRef != null && connectorRef.getRefCount() == 0) {
          connectorRef.server.removeConnector(connectorRef.connector);
          connectorRef.connector.stop();
          connectorRef.server.stop();
          connectorRef.memoryStore.stop();
          connectorRef.servlet = null;
          iterator.remove();
        }
      }
    }

    servlets.clear();
  }
}
