package de.fhg.aisec.dfpolicy;

public class LabelingRule extends Rule {
	
	private String attribute;
	
	public LabelingRule(String attribute, String label) {
		super.setLabel(label);
		this.setAttribute(attribute);
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
}
