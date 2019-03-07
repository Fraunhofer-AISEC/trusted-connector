package de.fhg.aisec.camel.multipart;

import de.fhg.aisec.ids.api.infomodel.InfoModelManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * The only purpose of this OSGi component is to connect to the InfoModelManager.
 * 
 * This is required for the MultipartComponent to use a proper IDS self description 
 * in the multipart messages.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(name = "ids-multipart-component")
public class MultipartComponent {

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private InfoModelManager infoModel = null;

	private static MultipartComponent instance;

	@Activate
	@SuppressWarnings("squid:S2696")
	protected void activate() {
		instance = this;
	}

	public static InfoModelManager getInfoModelManager() {
		return instance.infoModel;
	}

}
