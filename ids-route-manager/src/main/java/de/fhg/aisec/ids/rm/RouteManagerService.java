/*-
 * ========================LICENSE_START=================================
 * IDS Container Manager
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.management.DefaultManagementAgent;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.util.RouteStatDump;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.policy.PDP;
import de.fhg.aisec.ids.api.router.RouteComponent;
import de.fhg.aisec.ids.api.router.RouteException;
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteMetrics;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.rm.util.CamelRouteToDot;
import de.fhg.aisec.ids.rm.util.PrologPrinter;

/**
 * Manages Camel routes.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(enabled=true, immediate=true, name="ids-routemanager")
public class RouteManagerService implements RouteManager {
	private static final Logger LOG = LoggerFactory.getLogger(RouteManagerService.class);
	private PDP pdp;
	private ComponentContext ctx;

	@Activate
	protected void activate(ComponentContext ctx) {
		this.ctx = ctx;
	}
	
	@Reference(name="routemanager-camelcontext", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.MULTIPLE)
	public void bindCamelContext(CamelContext cCtx) {
		try {
			cCtx.stop();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		CamelInterceptor interceptor = new CamelInterceptor(this);
		cCtx.addInterceptStrategy(interceptor);		
		cCtx.setDefaultTracer(interceptor);
		for (Route r: cCtx.getRoutes()) {
			try {
				cCtx.stopRoute(r.getId());
				cCtx.startRoute(r.getId());
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
	
	public void unbindCamelContext(CamelContext cCtx) {
		LOG.info("unbound from CamelContext " + cCtx);		
	}
	
	@Reference(name="routemanager-pdp", policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.OPTIONAL)
	public void bindPdp(PDP pdp) {
		LOG.info("Bound to pdp " + pdp);
		this.pdp = pdp;
	}
	public void unbindPdp(PDP pdp) {
		LOG.warn("Policy decision point disappeared. All events will pass through uncontrolled.");
		this.pdp = null;
	}
	public PDP getPdp() {
		return pdp;
	}
	
	@Override
	public List<RouteObject> getRoutes() {
		List<RouteObject> result = new ArrayList<>();
		List<CamelContext> camelO = getCamelContexts();

		// Create response
		for (CamelContext cCtx : camelO) {
			for (RouteDefinition rd : cCtx.getRouteDefinitions()) {
				result.add(routeDefinitionToObject(cCtx, rd));
			}
		}
		return result;
	}
	
	@Override
	public void startRoute(String routeId) throws RouteException {
		List<CamelContext> camelC = getCamelContexts();

		for (CamelContext cCtx : camelC) {
			Route rt = cCtx.getRoute(routeId);
			if(rt != null) {
				try {
					cCtx.startRoute(routeId);
				} catch (Exception e) {
					throw new RouteException(e);
				}
			}
		}		
	}

	@Override
	public void stopRoute(String routeId) throws RouteException {
		List<CamelContext> camelC = getCamelContexts();

		for (CamelContext cCtx : camelC) {
			Route rt = cCtx.getRoute(routeId);
			if(rt != null) {
				try {
					cCtx.stopRoute(routeId);
				} catch (Exception e) {
					throw new RouteException(e);
				}
			}
		}		
	}

	@Override
	public List<RouteComponent> listComponents() {
		List<RouteComponent> componentNames = new ArrayList<>();
		BundleContext bCtx = FrameworkUtil.getBundle(RouteManagerService.class).getBundleContext();
		if (bCtx==null) {
			return componentNames;
		}
		
		try {
			ServiceReference<?>[] services = bCtx.getServiceReferences("org.apache.camel.spi.ComponentResolver", null);
			for (ServiceReference<?> sr : services) {
				String bundle = sr.getBundle().getHeaders().get("Bundle-Name");
				if (bundle==null || "".equals(bundle)) {
					bundle = sr.getBundle().getSymbolicName();
				}
				String description = sr.getBundle().getHeaders().get("Bundle-Description");
				if (description==null) {
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
	public Map<String, Collection<String>> getEndpoints() {
		List<CamelContext> camelO = getCamelContexts();
		return camelO.stream().collect(Collectors.toMap(c -> c.getName(), c -> c.getEndpoints()
				.stream()
				.map(ep -> ep.getEndpointUri())
				.collect(Collectors.toList())));
	}
	
	@Override
	public Map<String,String> listEndpoints() {
		List<CamelContext> camelO = getCamelContexts();
		Map<String,String> epURIs = new HashMap<>();
		
		for (CamelContext cCtx : camelO) {			
			for (Entry<String, Endpoint> e:cCtx.getEndpointMap().entrySet()) {
				epURIs.put(e.getKey(), e.getValue().getEndpointUri());
			}
		}
		
		return epURIs;			
	}
	
	@Override
	public Map<String,RouteMetrics> getRouteMetrics() {
		Map<String,RouteMetrics> rdump = new HashMap<>();
		List<CamelContext> cCtxs = getCamelContexts();
		for (CamelContext cCtx: cCtxs) {
			List<RouteDefinition> rds = cCtx.getRouteDefinitions();
			for (RouteDefinition rd: rds) {
				RouteStatDump stat;
				try {
					stat = this.getRouteStats(cCtx, rd);
				} catch (MalformedObjectNameException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | ReflectionException | JAXBException e) {
					LOG.error(e.getMessage(), e);
					continue;
				}
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
		}
		return rdump;
	}

	
	@Override
	public void addRoute(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void delRoute(String routeId) {
		List<CamelContext> cCtxs = getCamelContexts();
		for (CamelContext cCtx: cCtxs) {
			for (RouteDefinition rd : cCtx.getRouteDefinitions()) {
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

	@Override
	public void loadRoutes(String arg0) {
		// TODO Auto-generated method stub

	}

	private List<CamelContext> getCamelContexts() {
		List<CamelContext> camelContexts = new ArrayList<>();
        try {
            ServiceReference<?>[] references = this.ctx.getBundleContext().getServiceReferences(CamelContext.class.getName(), null);
            if (references != null) {
                for (ServiceReference<?> reference : references) {
                    if (reference != null) {
                        CamelContext camelContext = (CamelContext) this.ctx.getBundleContext().getService(reference);
                        if (camelContext != null) {
                            camelContexts.add(camelContext);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Cannot retrieve the list of Camel contexts.", e);
        }

        // sort the list
        Collections.sort(camelContexts, (ctx1, ctx2) -> ctx1.getName().compareTo(ctx2.getName()));
		return camelContexts;
	}

	/**
	 * Wraps a RouteDefinition in a RouteObject for use over API.
	 * 
	 * @param cCtx
	 * @param rd
	 * @return
	 */
	private RouteObject routeDefinitionToObject(CamelContext cCtx, RouteDefinition rd) {
		try {
			ModelHelper.dumpModelAsXml(cCtx, rd);
		} catch (JAXBException e) {
			LOG.error(e.getMessage(), e);
		}
		return new RouteObject(rd.getId(), rd.getDescriptionText(), routeToDot(rd), rd.getShortName(), cCtx.getName(), cCtx.getUptimeMillis(), cCtx.getRouteStatus(rd.getId()).toString());
	}
	
	/**
	 * Creates a visualization of a Camel route in DOT (graphviz) format.
	 *  
	 * @param rd
	 * @return
	 */
	private String routeToDot(RouteDefinition rd) {
		String result="";
		try {
			CamelRouteToDot viz = new CamelRouteToDot();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bos, "UTF-8"));
			viz.printSingleRoute(writer, rd);
			writer.flush();
			result = bos.toString("UTF-8");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return result;
	}
	
	protected RouteStatDump getRouteStats(CamelContext cCtx, RouteDefinition rd) throws MalformedObjectNameException, JAXBException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
		JAXBContext context = JAXBContext.newInstance(RouteStatDump.class);
		 Unmarshaller unmarshaller = context.createUnmarshaller();
		  ManagementAgent agent = cCtx.getManagementStrategy().getManagementAgent();
          if (agent != null) {
              MBeanServer mBeanServer = agent.getMBeanServer();
              Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(DefaultManagementAgent.DEFAULT_DOMAIN + ":type=routes,name=\"" + rd.getId() + "\",*"), null);
              for (ObjectName routeMBean : set) {
                  // the route must be part of the camel context
                  String camelId = (String) mBeanServer.getAttribute(routeMBean, "CamelId");
                  if (camelId != null && camelId.equals(cCtx.getName())) {
                      String xml = (String) mBeanServer.invoke(routeMBean, "dumpRouteStatsAsXml", new Object[]{Boolean.FALSE, Boolean.TRUE}, new String[]{"boolean", "boolean"});
                      return (RouteStatDump) unmarshaller.unmarshal(new StringReader(xml));
                  }
              }
          }
          return null;
	}

	@Override
	public String getRouteAsProlog(String routeId) {
		Optional<CamelContext> c = getCamelContexts()
				.parallelStream()
				.filter(cCtx -> cCtx.getRouteDefinition(routeId) != null)
				.findAny();
			
			if (c.isPresent()) {
				try {
					RouteDefinition rd = c.get().getRouteDefinition(routeId);
					StringWriter writer = new StringWriter();
					new PrologPrinter().printSingleRoute(writer, rd);
					writer.flush();
					return writer.getBuffer().toString();					
				} catch (IOException e) {
					LOG.error("Error printing route to prolog " + routeId, e);
				}
			}

			return "";
	}
	
	@Override
	public String getRouteAsString(String routeId) {
		Optional<CamelContext> c = getCamelContexts()
			.parallelStream()
			.filter(cCtx -> cCtx.getRouteDefinition(routeId) != null)
			.findAny();
		
		if (c.isPresent()) {
			RouteDefinition rd = c.get().getRouteDefinition(routeId);
			try {
				ModelHelper.dumpModelAsXml(c.get(), rd);
			} catch (JAXBException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
