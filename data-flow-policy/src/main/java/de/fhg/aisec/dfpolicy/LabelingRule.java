package de.fhg.aisec.dfpolicy;

public class LabelingRule {
	
	private String attribute;
	private String label;
	
	public LabelingRule(String attribute, String label) {
		this.attribute = attribute;
		this.label = label;
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}	

}
