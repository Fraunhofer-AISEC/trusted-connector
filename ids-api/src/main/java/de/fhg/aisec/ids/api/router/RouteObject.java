package de.fhg.aisec.ids.api.router;

public class RouteObject {
	private String status;
	private long uptime;
	private String ctxName;
	private String shortName;
	private String dotGraph;
	private String description;
	private String routeId;

	@SuppressWarnings("unused")
	private RouteObject() { /* Do not call me */ }
	
	public RouteObject(String routeId, String description, String dotGraph, String shortName, String ctxName, long uptime, String status) {
		this.routeId = routeId;
		this.description = description;
		this.dotGraph = dotGraph;
		this.shortName = shortName;
		this.ctxName = ctxName;
		this.uptime = uptime;
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public long getUptime() {
		return uptime;
	}

	public String getCtxName() {
		return ctxName;
	}

	public String getShortName() {
		return shortName;
	}

	public String getDotGraph() {
		return dotGraph;
	}

	public String getDescription() {
		return description;
	}

	public String getRouteId() {
		return routeId;
	}
}
