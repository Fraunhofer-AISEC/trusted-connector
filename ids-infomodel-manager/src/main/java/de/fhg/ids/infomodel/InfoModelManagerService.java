package de.fhg.ids.infomodel;

import de.fhg.aisec.ids.api.infomodel.InfoModelManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.prefs.PreferencesService;

/**
 * This is the most simple implementation of an infomodel-manager and likely to
 * be replaced in the future.
 * 
 * It uses mainly hardcoded JSON-LD strings and only fills in the blanks where
 * necessary.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(name = "ids-infomodel", immediate = true)
public class InfoModelManagerService implements InfoModelManager {

	/* We assume descriptive information about this connector is available 
	 * via the standard OSGi preferences service.
	 */
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private PreferencesService prefs;

	@Override
	public String getConnectorAsJsonLd() {
		return "{\n" + "  \"@type\" : \"https://schema.industrialdataspace.org/connector/Connector\",\n"
				+ "  \"https://schema.industrialdataspace.org/base/entityName\" : " + getEntityName() + ",\n"
				+ "  \"https://schema.industrialdataspace.org/base/entityDescription\" : " + getEntityDescription() + ",\n"
				+ "  \"https://schema.industrialdataspace.org/base/maintainer\" : " + getParticipantAsJsonLd() + ",\n"
				+ "  \"https://schema.industrialdataspace.org/base/operator\" :" + getOperatorAsJsonLd() + ",\n"
				+ "  \"https://schema.industrialdataspace.org/base/owner\" : " + getOwnerAsJsonLd() + ",\n"
				+ "  \"https://schema.industrialdataspace.org/base/supportedIMVersion\" : \"http://ids.semantic-interoperability.org\",\n"
				+ "  \"securityProfile\" : {\n"
				+ "    \"https://schema.industrialdataspace.org/securityProfile/basedOn\" : \"level1 security profile\"\n"
				+ "  }\n" + "}\n";
	}

	private String getParticipantAsJsonLd() {
		return "{\n " + "    \"https://schema.industrialdataspace.org/participant/corporateEmailAddress\" : \""
				+ prefs.getUserPreferences("ids").get("maintainer.corporateEmailAddress", "undefined").replace("\"", "") + "\","
				+ " \n" + "    \"https://schema.industrialdataspace.org/participant/corporateHomepage\" : \""
				+ prefs.getUserPreferences("ids").get("maintainer.corporateHomepage", "http://www.example.com").replace("\"", "") + "\""
				+ " \n" + "\n}\n";
	}

	private String getOperatorAsJsonLd() {
		return "{\n " + "    \"https://schema.industrialdataspace.org/participant/corporateEmailAddress\" : \""
				+ prefs.getUserPreferences("ids").get("operator.corporateEmailAddress", "undefined").replace("\"", "") + "\","
				+ " \n" + "    \"https://schema.industrialdataspace.org/participant/corporateHomepage\" : \""
				+ prefs.getUserPreferences("ids").get("operator.corporateHomepage", "http://www.example.com")
						.replace("\"", "") + "\""
				+ " \n" + "\n}\n";
	}

	private String getOwnerAsJsonLd() {
		return "{\n " 
				+ "    \"https://schema.industrialdataspace.org/participant/corporateEmailAddress\" : \""
				+ 		prefs.getUserPreferences("ids").get("owner.corporateEmailAddress", "undefined").replace("\"", "") + "\","
				+ " 	\n" 
				+ "    \"https://schema.industrialdataspace.org/participant/corporateHomepage\" : \""
				+ 		prefs.getUserPreferences("ids").get("owner.corporateHomepage", "http://www.example.com").replace("\"", "") + "\""
				+ "\n}\n";
	}
	
	private String getEntityName() {
		return "\"" + prefs.getUserPreferences("ids").get("connectorName", "My Connector").replace("\"", "") + "\"";
	}

	private String getEntityDescription() {
		return "\"" + prefs.getUserPreferences("ids").get("connectorDescription", "This is a non-productive test connector. Do not rely on this self-description.").replace("\"", "") + "\"";
	}
}
