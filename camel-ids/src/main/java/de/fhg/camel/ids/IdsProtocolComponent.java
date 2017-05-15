package de.fhg.camel.ids;

import java.util.Optional;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.conm.ConnectionManager;

/**
 * Component binding dynamically to the OSGi preferences service.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class IdsProtocolComponent {
	private static final Logger LOG = LoggerFactory.getLogger(IdsProtocolComponent.class);
	private static Optional<PreferencesService> prefService = Optional.empty();
	private static Optional<ConnectionManager> connectionManager = Optional.empty();

	@Reference(name = "camel-ids.config.service",
            service = PreferencesService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConfigurationService")
	public void bindConfigurationService(PreferencesService conf) {
		LOG.info("Bound to configuration service");
		IdsProtocolComponent.prefService = Optional.of(conf);
	}

	public void unbindConfigurationService(PreferencesService conf) {
		IdsProtocolComponent.prefService = Optional.empty();
	}
	
	public static Optional<PreferencesService> getPreferencesService() {
		return prefService;
	}
	
    @Reference(name = "connections.service",
            service = ConnectionManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConnectionManager")
    protected void bindConnectionManager(ConnectionManager conn) {
        LOG.info("Bound to connection manager");
        IdsProtocolComponent.connectionManager= Optional.of(conn);
    }

    protected void unbindConnectionManager(ConnectionManager conn) {
    	IdsProtocolComponent.connectionManager = Optional.empty();      
    }

	public static Optional<ConnectionManager> getConnectionManager() {
		return IdsProtocolComponent.connectionManager;
	}
}
