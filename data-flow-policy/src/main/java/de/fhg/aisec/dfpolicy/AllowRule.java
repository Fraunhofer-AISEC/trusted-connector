package de.fhg.aisec.dfpolicy;

public class AllowRule {
	
	private String label;
	private String destination;
	
	
	public AllowRule(String label, String destination) {
		this.label = label;
		this.destination = destination;
	}

	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
}
