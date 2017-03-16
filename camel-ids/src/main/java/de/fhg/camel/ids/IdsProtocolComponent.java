package de.fhg.camel.ids;

import java.util.Optional;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component binding dynamically to the OSGi preferences service.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class IdsProtocolComponent {
	private static final Logger LOG = LoggerFactory.getLogger(IdsProtocolComponent.class);
	private static Optional<PreferencesService> prefService;

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
}
