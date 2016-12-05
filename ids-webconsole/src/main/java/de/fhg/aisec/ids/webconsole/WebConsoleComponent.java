package de.fhg.aisec.ids.webconsole;


import java.util.Optional;

import org.apache.camel.Route;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.Registry;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.configuration.ConfigurationService;
import de.fhg.aisec.ids.api.router.RouteManager;

/**
 * IDS management console, reachable at http://localhost:8181/ids/ids.html.
 * 
 * This OSGi component registers a web application, as soon as it is bound to an
 * <code>HttpService</code> interface. The web application will allow to control
 * and configure the IDS Core Platform and the connected services.
 * 
 * Note that the web console does not enforce any authorization or encryption
 * and provides everyone with the ability to control highly critical settings of
 * the core platform. It must only be used for demonstration purposes and must
 * be deactivated and/or removed in productive use.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(name="ids-webconsole")
public class WebConsoleComponent {
	private static final Logger LOG = LoggerFactory.getLogger(WebConsoleComponent.class);
	private static Optional<ConfigurationService> configService = Optional.empty();
	private static Optional<ContainerManager> cml = Optional.empty();
	private static Optional<RouteManager> routeManagerService = Optional.empty();
	private static ComponentContext componentCtx;
	private static OsgiDefaultCamelContext camelContext;
	
	@Activate
	protected void activate(ComponentContext componentContext) throws Exception {
		LOG.info("IDS webconsole activated");
		WebConsoleComponent.componentCtx = componentContext;
		
		//Just for testing: Create camel context
		camelContext = new OsgiDefaultCamelContext(componentContext.getBundleContext(), createCamelRegistry());
        try {
            camelContext.start();
        } catch (Exception e) {
            LOG.error("Failed to start Camel", e);
        }
	}
	
	private Registry createCamelRegistry() {
        SimpleRegistry registry = new SimpleRegistry();
        return registry;
    }	

	@Deactivate
	protected void deactivate(ComponentContext componentContext) throws Exception {
		LOG.info("IDS webconsole deactivated");
		WebConsoleComponent.componentCtx = null;
	}
	
	@Reference(name = "cml.service",
            service = ContainerManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindContainerManagerService")
	protected void bindContainerManagerService(ContainerManager cml) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.cml= Optional.of(cml);
	}

	protected void unbindContainerManagerService(ContainerManager http) {
		WebConsoleComponent.cml = Optional.empty();		
	}
	
	public static Optional<ContainerManager> getContainerManager() {
		return WebConsoleComponent.cml;
	}

	@Reference(name = "config.service",
            service = ConfigurationService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConfigurationService")
	public void bindConfigurationService(ConfigurationService conf) {
		LOG.info("Bound to configuration service");
		WebConsoleComponent.configService = Optional.of(conf);
	}

	public void unbindConfigurationService(ConfigurationService conf) {
		WebConsoleComponent.configService = Optional.empty();
	}

	public static Optional<ConfigurationService> getConfigService() {
		return WebConsoleComponent.configService;
	}

	@Reference(name = "route.service",
            service = RouteManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindRouteManagerService")
	public void bindRouteManagerService(RouteManager router) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.routeManagerService = Optional.of(router);
	}

	public void unbindRouteManagerService(RouteManager router) {
		WebConsoleComponent.routeManagerService = Optional.empty();
	}

	public static Optional<RouteManager> getRouteManagerService() {
		return routeManagerService;
	}
	
	public static Optional<OsgiDefaultCamelContext> getCamelContext() {
		if (componentCtx==null) {
			return Optional.empty();
		}
		
		if (camelContext==null) {
			return Optional.empty();
		}
		// Create Apache Camel context
		
		LOG.info("Camel routes:");
		for (Route route : camelContext.getRoutes()) {
			LOG.info("  Route: " + route.getId());
		}
		return Optional.of(camelContext);
	}
}