package de.fhg.aisec.ids.api.policy;

import java.util.HashSet;
import java.util.Set;

public class ServiceNode {
	private String endpoint;
	private Set<String> properties;
	private Set<String> capabilities;
	
	public ServiceNode(String endpoint, Set<String> properties, Set<String> capabilities) {
		super();
		this.endpoint = endpoint;
		this.properties = properties!=null?properties:new HashSet<>();
		this.capabilities = capabilities!=null?capabilities:new HashSet<>();
	}
	
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public Set<String> getProperties() {
		return properties;
	}
	public void setProperties(Set<String> properties) {
		this.properties = properties;
	}
	public Set<String> getCapabilties() {
		return capabilities;
	}
	public void setCapabilties(Set<String> capabilities) {
		this.capabilities = capabilities;
	}
}
