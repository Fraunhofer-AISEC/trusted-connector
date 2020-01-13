/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
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
package de.fhg.aisec.ids.rm;

import de.fhg.aisec.ids.api.ReferenceUnbind;
import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.router.*;
import de.fhg.aisec.ids.rm.util.CamelRouteToDot;
import de.fhg.aisec.ids.rm.util.PrologPrinter;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.management.DefaultManagementAgent;
import org.apache.camel.model.*;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.support.dump.RouteStatDump;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Manages Camel routes.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
@Component(immediate = true, name = "ids-routemanager")
public class RouteManagerService implements RouteManager {
  private static final Logger LOG = LoggerFactory.getLogger(RouteManagerService.class);

  @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
  private volatile PDP pdp;

  private ComponentContext ctx;

  @Activate
  protected void activate(ComponentContext ctx) {
    this.ctx = ctx;
  }

  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  public void bindCamelContext(CamelContext cCtx) {
    try {
      cCtx.stop();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    CamelInterceptor interceptor = new CamelInterceptor(this);
    var routeController = cCtx.getRouteController();
    var ecc = cCtx.adapt(ExtendedCamelContext.class);
    ecc.addInterceptStrategy(interceptor);
    for (Route r : cCtx.getRoutes()) {
      try {
        routeController.stopRoute(r.getId());
        routeController.startRoute(r.getId());
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
    try {
      cCtx.start();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  @ReferenceUnbind
  public void unbindCamelContext(CamelContext cCtx) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Unbound from CamelContext " + cCtx);
    }
  }

  PDP getPdp() {
    return pdp;
  }

  @Override
  @NonNull
  public List<RouteObject> getRoutes() {
    List<RouteObject> result = new ArrayList<>();
    List<CamelContext> camelContexts = getCamelContexts();

    // Create response
    for (CamelContext cCtx : camelContexts) {
      var mcc = cCtx.adapt(ModelCamelContext.class);
      for (RouteDefinition rd : mcc.getRouteDefinitions()) {
        result.add(routeDefinitionToObject(cCtx, rd));
      }
    }
    return result;
  }

  @Override
  public RouteObject getRoute(@NonNull String id) {
    List<CamelContext> camelContexts = getCamelContexts();

    // Create response
    for (CamelContext cCtx : camelContexts) {
      var mcc = cCtx.adapt(ModelCamelContext.class);
      RouteDefinition rd = mcc.getRouteDefinition(id);
      if (rd != null) {
        return routeDefinitionToObject(cCtx, rd);
      }
    }

    return null;
  }

  @Override
  public void startRoute(@Nullable String routeId) throws RouteException {
    List<CamelContext> camelC = getCamelContexts();

    for (CamelContext cCtx : camelC) {
      Route rt = cCtx.getRoute(routeId);
      if (rt != null) {
        try {
          cCtx.getRouteController().startRoute(routeId);
        } catch (Exception e) {
          throw new RouteException(e);
        }
      }
    }
  }

  @Override
  public void stopRoute(@Nullable String routeId) throws RouteException {
    List<CamelContext> camelC = getCamelContexts();

    for (CamelContext cCtx : camelC) {
      Route rt = cCtx.getRoute(routeId);
      if (rt != null) {
        try {
          cCtx.getRouteController().stopRoute(routeId);
        } catch (Exception e) {
          throw new RouteException(e);
        }
      }
    }
  }

  @Override
  @NonNull
  public List<RouteComponent> listComponents() {
    List<RouteComponent> componentNames = new ArrayList<>();
    BundleContext bCtx = FrameworkUtil.getBundle(RouteManagerService.class).getBundleContext();
    if (bCtx == null) {
      return componentNames;
    }

    try {
      ServiceReference<?>[] services =
          bCtx.getServiceReferences("org.apache.camel.spi.ComponentResolver", null);
      for (ServiceReference<?> sr : services) {
        String bundle = sr.getBundle().getHeaders().get("Bundle-Name");
        if (bundle == null || "".equals(bundle)) {
          bundle = sr.getBundle().getSymbolicName();
        }
        String description = sr.getBundle().getHeaders().get("Bundle-Description");
        if (description == null) {
          description = "";
        }
        componentNames.add(new RouteComponent(bundle, description));
      }
    } catch (InvalidSyntaxException e) {
      LOG.error(e.getMessage(), e);
    }
    return componentNames;
  }

  @Override
  @NonNull
  public Map<String, Collection<String>> getEndpoints() {
    List<CamelContext> camelO = getCamelContexts();
    return camelO
        .stream()
        .collect(
            Collectors.toMap(
                CamelContext::getName,
                c ->
                    c.getEndpoints()
                        .stream()
                        .map(Endpoint::getEndpointUri)
                        .collect(Collectors.toList())));
  }

  @Override
  @NonNull
  public Map<String, String> listEndpoints() {
    Map<String, String> epURIs = new HashMap<>();

    for (CamelContext cCtx : getCamelContexts()) {
      for (Entry<? extends ValueHolder<String>, Endpoint> e : cCtx.getEndpointRegistry().entrySet()) {
        epURIs.put(e.getKey().get(), e.getValue().getEndpointUri());
      }
    }

    return epURIs;
  }

  @Override
  @NonNull
  public Map<String, RouteMetrics> getRouteMetrics() {
    Map<String, RouteMetrics> rdump = new HashMap<>();
    List<CamelContext> cCtxs = getCamelContexts();
    for (CamelContext cCtx : cCtxs) {
      var mcc = cCtx.adapt(ModelCamelContext.class);
      List<RouteDefinition> rds = mcc.getRouteDefinitions();
      for (RouteDefinition rd : rds) {
        RouteStatDump stat;
        try {
          stat = this.getRouteStats(cCtx, rd);
          if (stat != null) {
            RouteMetrics m = new RouteMetrics();
            m.setCompleted(stat.getExchangesCompleted());
            m.setRedeliveries(stat.getRedeliveries());
            m.setFailed(stat.getExchangesFailed());
            m.setFailuresHandled(stat.getFailuresHandled());
            m.setInflight(stat.getExchangesInflight());
            m.setMaxProcessingTime(stat.getMaxProcessingTime());
            m.setMinProcessingTime(stat.getMinProcessingTime());
            m.setMeanProcessingTime(stat.getMeanProcessingTime());
            rdump.put(rd.getId(), m);
          }
        } catch (MalformedObjectNameException
            | AttributeNotFoundException
            | InstanceNotFoundException
            | MBeanException
            | ReflectionException
            | JAXBException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
    return rdump;
  }

  @Override
  public void delRoute(@Nullable String routeId) {
    List<CamelContext> cCtxs = getCamelContexts();
    for (CamelContext cCtx : cCtxs) {
      var mcc = cCtx.adapt(ModelCamelContext.class);
      for (RouteDefinition rd : mcc.getRouteDefinitions()) {
        if (rd.getId().equals(routeId)) {
          try {
            cCtx.removeRoute(rd.getId());
          } catch (Exception e) {
            LOG.error(e.getMessage(), e);
          }
          return;
        }
      }
    }
  }

  @NonNull
  private List<CamelContext> getCamelContexts() {
    List<CamelContext> camelContexts = new ArrayList<>();
    try {
      ServiceReference<?>[] references =
          this.ctx.getBundleContext().getServiceReferences(CamelContext.class.getName(), null);
      if (references != null) {
        Arrays.stream(references)
            .map(this.ctx.getBundleContext()::getService)
            .filter(Objects::nonNull)
            .map(CamelContext.class::cast)
            .forEach(camelContexts::add);
      }
    } catch (Exception e) {
      LOG.warn("Cannot retrieve the list of Camel contexts.", e);
    }

    // sort the list
    camelContexts.sort(Comparator.comparing(CamelContext::getName));
    return camelContexts;
  }

  /**
   * Wraps a RouteDefinition in a RouteObject for use over API.
   *
   * @param cCtx Camel Context
   * @param rd The RouteDefinition to be transformed
   * @return The resulting RouteObject
   */
  private RouteObject routeDefinitionToObject(
      @NonNull CamelContext cCtx, @NonNull RouteDefinition rd) {
    return new RouteObject(
        rd.getId(),
        rd.getDescriptionText(),
        routeToDot(rd),
        rd.getShortName(),
        cCtx.getName(),
        cCtx.getUptimeMillis(),
        cCtx.getRouteController().getRouteStatus(rd.getId()).toString());
  }

  /**
   * Creates a visualization of a Camel route in DOT (graphviz) format.
   *
   * @param rd The route definition to process
   * @return The string representation of the Camel route in DOT
   */
  @NonNull
  private String routeToDot(@NonNull RouteDefinition rd) {
    String result = "";
    try {
      CamelRouteToDot viz = new CamelRouteToDot();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8));
      viz.printSingleRoute(writer, rd);
      writer.flush();
      result = bos.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    return result;
  }

  @Override
  @NonNull
  public List<String> getRouteInputUris(@NonNull String routeId) {
    for (CamelContext ctx : getCamelContexts()) {
      var mcc = ctx.adapt(ModelCamelContext.class);
      for (RouteDefinition rd : mcc.getRouteDefinitions()) {
        if (routeId.equals(rd.getId())) {
          return Collections.singletonList(rd.getInput().getUri());
        }
      }
    }
    return Collections.emptyList();
  }

  protected RouteStatDump getRouteStats(CamelContext cCtx, RouteDefinition rd)
      throws MalformedObjectNameException, JAXBException, AttributeNotFoundException,
          InstanceNotFoundException, MBeanException, ReflectionException {
    JAXBContext context = JAXBContext.newInstance(RouteStatDump.class);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    ManagementAgent agent = cCtx.getManagementStrategy().getManagementAgent();
    if (agent != null) {
      MBeanServer mBeanServer = agent.getMBeanServer();
      Set<ObjectName> set =
          mBeanServer.queryNames(
              new ObjectName(
                  DefaultManagementAgent.DEFAULT_DOMAIN
                      + ":type=routes,name=\""
                      + rd.getId()
                      + "\",*"),
              null);
      for (ObjectName routeMBean : set) {
        // the route must be part of the camel context
        String camelId = (String) mBeanServer.getAttribute(routeMBean, "CamelId");
        if (camelId != null && camelId.equals(cCtx.getName())) {
          String xml =
              (String)
                  mBeanServer.invoke(
                      routeMBean,
                      "dumpRouteStatsAsXml",
                      new Object[] {Boolean.FALSE, Boolean.TRUE},
                      new String[] {"boolean", "boolean"});
          return (RouteStatDump) unmarshaller.unmarshal(new StringReader(xml));
        }
      }
    }
    return null;
  }

  /**
   * Retrieves the Prolog representation of a route
   *
   * @param routeId The id of the route that is to be exported
   */
  @Override
  @NonNull
  public String getRouteAsProlog(@NonNull String routeId) {
    Optional<CamelContext> c =
        getCamelContexts()
            .parallelStream()
            .filter(cCtx -> cCtx.adapt(ModelCamelContext.class).getRouteDefinition(routeId) != null)
            .findAny();

    if (c.isPresent()) {
      try {
        RouteDefinition rd = c.get().adapt(ModelCamelContext.class).getRouteDefinition(routeId);
        StringWriter writer = new StringWriter();
        new PrologPrinter().printSingleRoute(writer, rd);
        writer.flush();
        return writer.toString();
      } catch (IOException e) {
        LOG.error("Error printing route to prolog " + routeId, e);
      }
    }

    return "";
  }

  /**
   * Retrieves the textual representation of a route
   *
   * @param routeId The id of the route that is to be exported
   */
  @Override
  @Nullable
  public String getRouteAsString(@NonNull String routeId) {
    for (CamelContext c : getCamelContexts()) {
      RouteDefinition rd = c.adapt(ModelCamelContext.class).getRouteDefinition(routeId);
      if (rd == null) {
        continue;
      }
      try {
        return ModelHelper.dumpModelAsXml(c, rd);
      } catch (JAXBException e) {
        LOG.error(e.getMessage(), e);
      }
    }
    return null;
  }

  /**
   * Save a route, replacing it with a new representation within the same context
   *
   * @param routeId ID of the route to save
   * @param routeRepresentation The new textual representation of the route (XML etc.)
   * @throws RouteException If the route does not exist or some Exception was thrown during route
   *     replacement.
   */
  @Override
  @NonNull
  public RouteObject saveRoute(@NonNull String routeId, @NonNull String routeRepresentation)
      throws RouteException {
    LOG.debug("Save route \"" + routeId + "\": " + routeRepresentation);

    CamelContext cCtx = null;
    boolean routeStarted = false;

    // Find the state and CamelContext of the route to be saved
    for (CamelContext c : getCamelContexts()) {
      Route targetRoute = c.getRoute(routeId);
      if (targetRoute != null) {
        cCtx = c;
        ServiceStatus serviceStatus = cCtx.getRouteController().getRouteStatus(routeId);
        routeStarted =
            serviceStatus == ServiceStatus.Started || serviceStatus == ServiceStatus.Starting;
        break;
      }
    }
    if (cCtx == null) {
      LOG.error("Could not find route with id \"" + routeId + "\"");
      throw new RouteException("Could not find route with id \"" + routeId + "\"");
    }

    // Check for validity of route representation
    List<RouteDefinition> routes;
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(routeRepresentation.getBytes(StandardCharsets.UTF_8))) {
      // Load route(s) from XML
      RoutesDefinition rd = ModelHelper.loadRoutesDefinition(cCtx, bis);
      routes = rd.getRoutes();
      Optional<String> id =
          routes.stream().map(RouteDefinition::getId).filter(rid -> !routeId.equals(rid)).findAny();
      if (id.isPresent()) {
        throw new Exception(
            "The new route representation has a different ID: "
                + "Expected \""
                + routeId
                + "\" but got \""
                + id.get()
                + "\"");
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RouteException(e);
    }

    // Remove old route from CamelContext
    try {
      cCtx.removeRoute(routeId);
    } catch (Exception e) {
      LOG.error("Error while removing old route \"" + routeId + "\"", e);
      throw new RouteException(e);
    }

    // Add new route and start it if it was started/starting before save
    try {
      RouteDefinition routeDefinition = routes.get(0);
      cCtx.adapt(ModelCamelContext.class).addRouteDefinition(routeDefinition);
      if (routeStarted) {
        cCtx.getRouteController().startRoute(routeDefinition.getId());
      }
      return routeDefinitionToObject(cCtx, routeDefinition);
    } catch (Exception e) {
      LOG.error("Error while adding new route \"" + routeId + "\"", e);
      throw new RouteException(e);
    }
  }

  /**
   * Create a new route in a fresh context from text
   *
   * @param routeRepresentation The textual representation of the route to be inserted
   * @throws RouteException If a route with that name already exists
   */
  @Override
  public void addRoute(@NonNull String routeRepresentation) throws RouteException {
    LOG.debug("Adding new route: " + routeRepresentation);
    List<RouteObject> existingRoutes = this.getRoutes();
    // @todo: Need to verify that this or the call to CamelContext.start() below actually registers
    // the route properly
    CamelContext cCtx = new DefaultCamelContext();
    try (ByteArrayInputStream bis =
        new ByteArrayInputStream(routeRepresentation.getBytes(StandardCharsets.UTF_8))) {
      // Load route(s) from XML
      RoutesDefinition rd = ModelHelper.loadRoutesDefinition(cCtx, bis);
      List<RouteDefinition> routes = rd.getRoutes();
      // Check that intersection of existing and new routes is empty (=we do not allow overwriting
      // existing route ids)
      List<String> intersect =
          routes
              .stream()
              .filter(r -> existingRoutes.stream().anyMatch(er -> er.getId().equals(r.getId())))
              .map(OptionalIdentifiedDefinition::getId)
              .collect(Collectors.toList());
      if (!intersect.isEmpty()) {
        throw new RouteException(
            "Route id already exists. Will not overwrite it. " + String.join(", ", intersect));
      }
      cCtx.adapt(ModelCamelContext.class).addRouteDefinitions(routes);
      cCtx.start();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RouteException(e);
    }
  }
}
