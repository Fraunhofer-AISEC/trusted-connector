package de.fhg.aisec.ids.api.router;

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
