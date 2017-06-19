package de.fhg.aisec.ids.api.router;

/**
 * Representation of a "route component", i.e. a protocol adapter to attach
 * route endpoints to external services.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class RouteComponent {
	private String bundle;
	private String description;

	private RouteComponent() {
		/* Do not call me */
	}

	public RouteComponent(String bundleName, String description) {
		this.bundle = bundleName;
		this.description = description;
	}

	public String getBundle() {
		return bundle;
	}

	public String getDescription() {
		return description;
	}
}
