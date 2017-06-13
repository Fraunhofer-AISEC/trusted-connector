package de.fhg.aisec.ids.rm;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.model.RouteDefinition;
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
import de.fhg.aisec.ids.api.router.RouteManager;
import de.fhg.aisec.ids.api.router.RouteObject;
import de.fhg.aisec.ids.rm.util.CamelRouteToDot;

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
	public void startRoute(String routeId) throws Exception {
		List<CamelContext> camelC = getCamelContexts();

		for (CamelContext cCtx : camelC) {
			Route rt = cCtx.getRoute(routeId);
			if(rt != null) {
				cCtx.startRoute(routeId);
			}
		}		
	}

	@Override
	public void stopRoute(String routeId) throws Exception {
		List<CamelContext> camelC = getCamelContexts();

		for (CamelContext cCtx : camelC) {
			Route rt = cCtx.getRoute(routeId);
			if(rt != null) {
				cCtx.stopRoute(routeId);
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
		return camelO.stream().collect(Collectors.toMap(ctx -> ctx.getName(), ctx -> ctx.getEndpoints()
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
	public void addRoute(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean createConsumingEndpoint(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void delRoute(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRouteConfigAsString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadRoutes(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void provide(String arg0, ByteBuffer arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeConsumingEndpoint(String arg0) {
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
		// TODO Message count expected in frontend but here we only pass 0
		return new RouteObject(rd.getId(), rd.getDescriptionText(), routeToDot(rd), rd.getShortName(), cCtx.getName(), cCtx.getUptimeMillis(), cCtx.getRouteStatus(rd.getId()).toString(), 0);
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
}