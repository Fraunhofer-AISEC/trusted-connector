package de.fhg.aisec.dfpolicy;

import java.util.Set;

public class LabelingRule extends Rule {
	
	private String attribute;
	
	public LabelingRule(String attribute, Set<String> label) {
		System.out.println("Labeling Rule 1: " + label.size());
		super.setLabel(label);
		System.out.println("Labeling Rule 4: " + label.size());
		this.setAttribute(attribute);
		System.out.println("Labeling Rule 5");
	}
	
	public String getAttribute() {
		return attribute;
	}
	
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
}
