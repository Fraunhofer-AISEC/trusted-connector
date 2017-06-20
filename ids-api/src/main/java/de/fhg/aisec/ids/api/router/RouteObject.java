package de.fhg.aisec.ids.api.router;

/**
 * Bean representing a "route" (e.g., an Apache Camel route)
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class RouteObject {
	private String status;
	private long uptime;
	private String context;
	private String shortName;
	private String dot;
	private String description;
	private String id;

	public RouteObject() { /* Bean std c'tor */	}
	
	public RouteObject(String id, String description, String dot, String shortName, String context, long uptime, String status) {
		this.id = id;
		this.description = description;
		this.dot = dot;
		this.shortName = shortName;
		this.context = context;
		this.uptime = uptime;
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public long getUptime() {
		return uptime;
	}

	public String getContext() {
		return context;
	}

	public String getShortName() {
		return shortName;
	}

	public String getDot() {
		return dot;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setUptime(long uptime) {
		this.uptime = uptime;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public void setDot(String dot) {
		this.dot = dot;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(String id) {
		this.id = id;
	}
}
