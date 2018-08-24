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

import de.fhg.camel.ids.ProxyX509TrustManager;
import de.fhg.ids.comm.CertificatePair;
import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.spi.Metadata;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WebsocketComponent extends DefaultComponent implements SSLContextParametersAware {
  protected static final Logger LOG = LoggerFactory.getLogger(WebsocketComponent.class);
  protected static final Map<String, ConnectorRef> CONNECTORS = new HashMap<>();

  protected Map<String, WebSocketFactory> socketFactories;
  private final CertificatePair certificatePair = new CertificatePair();

  @Metadata(label = "security")
  protected SSLContextParameters sslContextParameters;

  @Metadata(label = "security", defaultValue = "false")
  protected boolean useGlobalSslContextParameters;

  @Metadata(label = "advanced")
  protected ThreadPool threadPool;

  @Metadata(defaultValue = "9292")
  protected Integer port = 9292;

  @Metadata(label = "advanced")
  protected Integer minThreads;

  @Metadata(label = "advanced")
  protected Integer maxThreads;

  @Metadata(defaultValue = "0.0.0.0")
  protected String host = "0.0.0.0";

  @Metadata(label = "consumer")
  protected String staticResources;

  @Metadata(label = "security", secret = true)
  protected String sslKeyPassword;

  @Metadata(label = "security", secret = true)
  protected String sslPassword;

  @Metadata(label = "security", secret = true)
  protected String sslKeystore;

  /**
   * Map for storing servlets. {@link WebsocketComponentServlet} is identified by pathSpec {@link
   * String}.
   */
  private Map<String, WebsocketComponentServlet> servlets = new HashMap<>();

  public class ConnectorRef {
    Server server;
    ServerConnector connector;
    WebsocketComponentServlet servlet;
    MemoryWebsocketStore memoryStore;
    AtomicInteger refCount = new AtomicInteger(1);

    ConnectorRef(
        Server server,
        ServerConnector connector,
        WebsocketComponentServlet servlet,
        MemoryWebsocketStore memoryStore) {
      this.server = server;
      this.connector = connector;
      this.servlet = servlet;
      this.memoryStore = memoryStore;
    }

    public int increment() {
      return refCount.incrementAndGet();
    }

    public int decrement() {
      return refCount.decrementAndGet();
    }

    public int getRefCount() {
      return refCount.get();
    }

    public MemoryWebsocketStore getMemoryStore() {
      return memoryStore;
    }

    public WebsocketComponentServlet getServlet() {
      return servlet;
    }

    public ServerConnector getConnector() {
      return connector;
    }
  }

  public WebsocketComponent() {
    this.setUseGlobalSslContextParameters(true);
    // this will automatically set up the ids handler factory
    this.setSocketFactories(new HashMap<>());
  }

  /** Connects the URL specified on the endpoint to the specified processor. */
  public void connect(WebsocketProducerConsumer prodcon) throws Exception {
    WebsocketEndpoint endpoint = prodcon.getEndpoint();

    String connectorKey = getConnectorKey(endpoint);

    synchronized (CONNECTORS) {
      ConnectorRef connectorRef = CONNECTORS.get(connectorKey);
      if (connectorRef == null) {
        // Create Server and add connector
        Server server = createServer();

        ServerConnector connector =
            getSocketConnector(server, endpoint.getSslContextParameters());

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

        MemoryWebsocketStore memoryStore = new MemoryWebsocketStore();

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
      WebsocketComponentServlet servlet = addServlet(sync, prodcon, endpoint.getResourceUri());
      if (prodcon instanceof WebsocketConsumer) {
        WebsocketConsumer consumer = (WebsocketConsumer) prodcon;
        if (servlet.getConsumer() == null) {
          servlet.setConsumer(consumer);
        }
        // register the consumer here
        servlet.connect(consumer);
      }
      if (prodcon instanceof WebsocketProducer) {
        WebsocketProducer producer = (WebsocketProducer) prodcon;
        producer.setStore(connectorRef.memoryStore);
      }
    }
  }

  /** Disconnects the URL specified on the endpoint from the specified processor. */
  public void disconnect(WebsocketProducerConsumer prodcon) throws Exception {
    // If the connector is not needed anymore then stop it
    WebsocketEndpoint endpoint = prodcon.getEndpoint();
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
        if (prodcon instanceof WebsocketConsumer) {
          connectorRef.servlet.disconnect((WebsocketConsumer) prodcon);
        }
        if (prodcon instanceof WebsocketProducer) {
          ((WebsocketProducer) prodcon).setStore(null);
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

    WebsocketEndpoint endpoint = new WebsocketEndpoint(this, uri, remaining, parameters);

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
      ServletContextHandler context, WebsocketEndpoint endpoint) {
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

  protected Server createStaticResourcesServer(
      Server server, ServletContextHandler context, String home) throws Exception {

    context.setContextPath("/");

    SessionHandler sh = new SessionHandler();
    context.setSessionHandler(sh);

    if (home != null) {
      String[] resources = home.split(":");
      if (LOG.isDebugEnabled()) {
        LOG.debug(">>> Protocol found: {}, and resource: {}", resources[0], resources[1]);
      }

      if (resources[0].equals("classpath")) {
        context.setBaseResource(
            new JettyClassPathResource(getCamelContext().getClassResolver(), resources[1]));
      } else if (resources[0].equals("file")) {
        context.setBaseResource(Resource.newResource(resources[1]));
      }
      DefaultServlet defaultServlet = new DefaultServlet();
      ServletHolder holder = new ServletHolder(defaultServlet);

      // avoid file locking on windows
      // http://stackoverflow.com/questions/184312/how-to-make-jetty-dynamically-load-static-pages
      holder.setInitParameter("useFileMappedBuffer", "false");
      context.addServlet(holder, "/");
    }

    server.setHandler(context);

    return server;
  }

  protected Server createStaticResourcesServer(
      ServletContextHandler context, String host, int port, String home) throws Exception {
    Server server = new Server();
    HttpConfiguration httpConfig = new HttpConfiguration();
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setHost(host);
    connector.setPort(port);
    server.addConnector(connector);
    return createStaticResourcesServer(server, context, home);
  }

  protected WebsocketComponentServlet addServlet(
      NodeSynchronization sync,
      WebsocketProducerConsumer prodcon,
      String resourceUri)
      throws Exception {

    // Get Connector from one of the Jetty Instances to add WebSocket Servlet
    WebsocketEndpoint endpoint = prodcon.getEndpoint();
    String key = getConnectorKey(endpoint);
    ConnectorRef connectorRef = getConnectors().get(key);

    WebsocketComponentServlet servlet;

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

  protected WebsocketComponentServlet createServlet(
      NodeSynchronization sync,
      String pathSpec,
      Map<String, WebsocketComponentServlet> servlets,
      ServletContextHandler handler) {
    WebsocketComponentServlet servlet = new WebsocketComponentServlet(sync, pathSpec, getSocketFactories());
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

  private ServerConnector getSocketConnector(Server server, SSLContextParameters sslContextParameters)
      throws GeneralSecurityException, IOException {
    if (sslContextParameters == null) {
      sslContextParameters = retrieveGlobalSslContextParameters();
    }
    if (sslContextParameters != null) {
      try {
        ProxyX509TrustManager.bindCertificatePair(sslContextParameters, true, certificatePair);
      } catch (GeneralSecurityException | IOException e) {
        LOG.error("Failed to patch TrustManager for WebsocketComponent", e);
      }
      SslContextFactory sslContextFactory = new SslContextFactory();
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

  private static String getConnectorKey(WebsocketEndpoint endpoint) {
    return endpoint.getProtocol() + ":" + endpoint.getHost() + ":" + endpoint.getPort();
  }

  private void applyCrossOriginFiltering(
      WebsocketEndpoint endpoint, ServletContextHandler context) {
    if (endpoint.isCrossOriginFilterOn()) {
      FilterHolder filterHolder = new FilterHolder();
      CrossOriginFilter filter = new CrossOriginFilter();
      filterHolder.setFilter(filter);
      filterHolder.setInitParameter("allowedOrigins", endpoint.getAllowedOrigins());
      context.addFilter(
          filterHolder, endpoint.getFilterPath(), EnumSet.allOf(DispatcherType.class));
    }
  }

  // Properties
  // -------------------------------------------------------------------------

  public String getStaticResources() {
    return staticResources;
  }

  /**
   * Set a resource path for static resources (such as .html files etc).
   *
   * <p>The resources can be loaded from classpath, if you prefix with <tt>classpath:</tt>,
   * otherwise the resources is loaded from file system or from JAR files.
   *
   * <p>For example to load from root classpath use <tt>classpath:.</tt>, or
   * <tt>classpath:WEB-INF/static</tt>
   *
   * <p>If not configured (eg <tt>null</tt>) then no static resource is in use.
   */
  public void setStaticResources(String staticResources) {
    this.staticResources = staticResources;
  }

  public String getHost() {
    return host;
  }

  /** The hostname. The default value is <tt>0.0.0.0</tt> */
  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  /** The port number. The default value is <tt>9292</tt> */
  public void setPort(Integer port) {
    this.port = port;
  }

  public String getSslKeyPassword() {
    return sslKeyPassword;
  }

  public String getSslPassword() {
    return sslPassword;
  }

  public String getSslKeystore() {
    return sslKeystore;
  }

  /** The password for the keystore when using SSL. */
  public void setSslKeyPassword(String sslKeyPassword) {
    this.sslKeyPassword = sslKeyPassword;
  }

  /** The password when using SSL. */
  public void setSslPassword(String sslPassword) {
    this.sslPassword = sslPassword;
  }

  /** The path to the keystore. */
  public void setSslKeystore(String sslKeystore) {
    this.sslKeystore = sslKeystore;
  }

  public Integer getMinThreads() {
    return minThreads;
  }

  /**
   * To set a value for minimum number of threads in server thread pool. MaxThreads/minThreads or
   * threadPool fields are required due to switch to Jetty9. The default values for minThreads is 1.
   */
  public void setMinThreads(Integer minThreads) {
    this.minThreads = minThreads;
  }

  public Integer getMaxThreads() {
    return maxThreads;
  }

  /**
   * To set a value for maximum number of threads in server thread pool. MaxThreads/minThreads or
   * threadPool fields are required due to switch to Jetty9. The default values for maxThreads is 1
   * + 2 * noCores.
   */
  public void setMaxThreads(Integer maxThreads) {
    this.maxThreads = maxThreads;
  }

  public ThreadPool getThreadPool() {
    return threadPool;
  }

  /**
   * To use a custom thread pool for the server. MaxThreads/minThreads or threadPool fields are
   * required due to switch to Jetty9.
   */
  public void setThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  public SSLContextParameters getSslContextParameters() {
    return sslContextParameters;
  }

  /** To configure security using SSLContextParameters */
  public void setSslContextParameters(SSLContextParameters sslContextParameters) {
    this.sslContextParameters = sslContextParameters;
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
      this.socketFactories.put("ids", new DefaultWebsocketFactory(certificatePair));
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
    if (CONNECTORS.size() > 0) {
      for (ConnectorRef connectorRef : CONNECTORS.values()) {
        if (connectorRef != null && connectorRef.getRefCount() == 0) {
          connectorRef.server.removeConnector(connectorRef.connector);
          connectorRef.connector.stop();
          connectorRef.server.stop();
          connectorRef.memoryStore.stop();
          connectorRef.servlet = null;
        }
      }
    }
    CONNECTORS.clear();

    servlets.clear();
  }
}
