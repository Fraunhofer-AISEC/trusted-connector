package de.fhg.aisec.dfpolicy;

public class AllowRule {
	
	private String label;
	private String destination;
	
	public AllowRule(String destination, String label) {
		this.destination = destination;
		this.label = label;
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
	@Override
	public String toString() {
		return "AllowRule [label=" + label + ", destination=" + destination + "]";
	}

}
