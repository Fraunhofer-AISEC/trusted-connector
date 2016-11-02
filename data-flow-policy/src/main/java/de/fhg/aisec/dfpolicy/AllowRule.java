package de.fhg.aisec.dfpolicy;

import java.util.Set;

public class AllowRule extends Rule {

	private String destination;
	
	public AllowRule(Set<String> label, String destination) {
		super.setLabel(label);
		this.setDestination(destination);
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
}
