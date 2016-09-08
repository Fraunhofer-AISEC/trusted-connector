package de.fhg.aisec.ids.webconsole;

import org.osgi.service.component.annotations.Component;
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
 * and provides everyone with the ability to control highly critical setting of
 * the core platform. It must only be used for demonstration purposes and must
 * be deactivated and/or removed in productive use.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */

@Component(name="ids-webconsole")
public class WebConsoleComponent {
	private static final Logger LOG = LoggerFactory.getLogger(WebConsoleComponent.class);
	private static ConfigurationService configService;
	private static ContainerManager cml;
	private static RouteManager routeManagerService;
	
	@Reference(name = "cml.service",
            service = ContainerManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindContainerManagerService")
	protected void bindContainerManagerService(ContainerManager cml) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.cml= cml;
	}

	protected void unbindContainerManagerService(ContainerManager http) {
		WebConsoleComponent.cml = null;		
	}
	
	public static ContainerManager getContainerManager() {
		return WebConsoleComponent.cml;
	}

	@Reference(name = "config.service",
            service = ConfigurationService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConfigurationService")
	public void bindConfigurationService(ConfigurationService conf) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.configService = conf;
	}

	public void unbindConfigurationService(ConfigurationService conf) {
		WebConsoleComponent.configService = null;
	}

	public static ConfigurationService getConfigService() {
		return WebConsoleComponent.configService;
	}

	@Reference(name = "route.service",
            service = RouteManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindRouteManagerService")
	public void bindRouteManagerService(RouteManager router) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.routeManagerService = router;
	}

	public void unbindRouteManagerService(RouteManager router) {
		WebConsoleComponent.routeManagerService = null;
	}

	public static RouteManager getRouteManagerService() {
		return routeManagerService;
	}
}