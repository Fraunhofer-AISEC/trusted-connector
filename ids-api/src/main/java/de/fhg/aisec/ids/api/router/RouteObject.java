package de.fhg.aisec.ids.api.router;

public class RouteObject {
	private String status;
	private long uptime;
	private String context;
	private String shortName;
	private String dot;
	private String description;
	private String id;
	private long messages;

	@SuppressWarnings("unused")
	private RouteObject() { /* Do not call me */ }
	
	public RouteObject(String id, String description, String dot, String shortName, String context, long uptime, String status, long messages) {
		this.id = id;
		this.description = description;
		this.dot = dot;
		this.shortName = shortName;
		this.context = context;
		this.uptime = uptime;
		this.status = status;
		this.messages = messages;
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
	
	public long getMessages() {
		return messages;
	}
}
