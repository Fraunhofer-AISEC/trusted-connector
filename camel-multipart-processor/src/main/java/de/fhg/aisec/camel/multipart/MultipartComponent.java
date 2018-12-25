package de.fhg.aisec.camel.multipart;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import de.fhg.aisec.ids.api.infomodel.InfoModelManager;

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
	protected static InfoModelManager infoModel;
}
