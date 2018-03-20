/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole;


import java.util.Optional;

import com.google.gson.Gson;
import de.fhg.aisec.ids.webconsole.api.ConfigApi;
import de.fhg.aisec.ids.webconsole.api.data.ConnectionSettings;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.Constants;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.api.router.RouteManager;

import javax.ws.rs.ServiceUnavailableException;

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
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */

@Component(name="ids-webconsole")
public class WebConsoleComponent {
	private static final Logger LOG = LoggerFactory.getLogger(WebConsoleComponent.class);
	private static PreferencesService preferencesService = null;
	private static ContainerManager cml = null;
	private static RouteManager rm = null;
	private static ConnectionManager connectionManager = null;
	private static PAP pap;
	private static ComponentContext componentCtx = null;
	
	@Activate
	protected void activate(ComponentContext componentContext) {
		LOG.info("IDS webconsole activated");
		WebConsoleComponent.componentCtx = componentContext;
	}
	
	@Deactivate
	protected void deactivate(ComponentContext componentContext) throws Exception {
		LOG.info("IDS webconsole deactivated");
		WebConsoleComponent.componentCtx = null;
	}
	
	public static Optional<ComponentContext> getComponentContext() {
		return Optional.ofNullable(componentCtx);
	}

	public static ComponentContext getComponentContextOrThrowSUE() {
		if (componentCtx != null) {
			return componentCtx;
		} else {
			throw new ServiceUnavailableException("ConnectionManager is currently not available");
		}
	}
		
	@Reference(name = "cml.service",
            service = ContainerManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindContainerManagerService")
	protected void bindContainerManagerService(ContainerManager cml) {
		LOG.info("Bound to container manager");
		WebConsoleComponent.cml = cml;
	}
	
	
	protected void unbindContainerManagerService(ContainerManager http) {
		WebConsoleComponent.cml = null;
	}
	
	public static Optional<ContainerManager> getContainerManager() {
		return Optional.ofNullable(cml);
	}

	public static ContainerManager getContainerManagerOrThrowSUE() {
		if (cml != null) {
			return cml;
		} else {
			throw new ServiceUnavailableException("ConnectionManager is currently not available");
		}
	}
	
    @Reference(name = "connections.service",
            service = ConnectionManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConnectionManager")
    protected void bindConnectionManager(ConnectionManager conn) {
        LOG.info("Bound to connection manager");
        WebConsoleComponent.connectionManager = conn;
    }

    protected void unbindConnectionManager(ConnectionManager conn) {
        WebConsoleComponent.connectionManager = null;
    }

	public static Optional<ConnectionManager> getConnectionManager() {
		return Optional.ofNullable(connectionManager);
	}

	public static ConnectionManager getConnectionManagerOrThrowSUE() {
		if (connectionManager != null) {
			return connectionManager;
		} else {
			throw new ServiceUnavailableException("ConnectionManager is currently not available");
		}
	}

	@Reference(name = "config.service",
            service = PreferencesService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConfigurationService")
	public void bindConfigurationService(PreferencesService conf) {
		LOG.info("Bound to configuration service");
		preferencesService = conf;
		// Create generic ConnectionSettings, if not existing
		Preferences prefs = conf.getUserPreferences(Constants.CONNECTIONS_PREFERENCES);
		if(prefs != null && prefs.get(ConfigApi.GENERAL_CONFIG, null) == null) {
			prefs.put(ConfigApi.GENERAL_CONFIG, new Gson().toJson(new ConnectionSettings()));
		}
	}

	public void unbindConfigurationService(PreferencesService conf) {
		preferencesService = null;
	}

	public static Optional<PreferencesService> getConfigService() {
		return Optional.ofNullable(preferencesService);
	}

	public static PreferencesService getPreferencesServiceOrThrowSUE() {
		if (preferencesService != null) {
			return preferencesService;
		} else {
			throw new ServiceUnavailableException("PreferenceService is currently not available");
		}
	}
	
	@Reference(name = "rm.service",
            service = RouteManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindRouteManagerService")
	protected void bindRouteManagerService(RouteManager rm) {
		LOG.info("Bound to route manager");
		WebConsoleComponent.rm  = rm;
	}

	protected void unbindRouteManagerService(RouteManager rm) {
		WebConsoleComponent.rm = null;
	}
	
	public static Optional<RouteManager> getRouteManager() {
		return Optional.ofNullable(rm);
	}

	public static RouteManager getRouteManagerOrThrowSUE() {
		if (rm != null) {
			return rm;
		} else {
			throw new ServiceUnavailableException("RouteManager is currently not available");
		}
	}

	@Reference(name = "pap.service",
            service = PAP.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindPolicyAdministrationPoint")
	protected void bindPolicyAdministrationPoint(PAP pap) {
		LOG.info("Bound to policy administration point");
		WebConsoleComponent.pap = pap;
	}

	protected void unbindPolicyAdministrationPoint(PAP pap) {
		WebConsoleComponent.pap = null;
	}
	
	public static Optional<PAP> getPolicyAdministrationPoint() {
		return Optional.ofNullable(pap);
	}

	public static PAP getPolicyAdministrationPointOrThrowSUE() {
		if (pap != null) {
			return pap;
		} else {
			throw new ServiceUnavailableException("PAP is currently not available");
		}
	}
}
