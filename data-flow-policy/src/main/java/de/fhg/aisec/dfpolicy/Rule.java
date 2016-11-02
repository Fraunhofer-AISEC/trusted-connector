package de.fhg.aisec.dfpolicy;

import java.util.Set;

public class Rule {
	
	private Set<String> label;
	
	public Set<String> getLabel() {
		return label;
	}
	
	public void setLabel(Set<String> label) {
		System.out.println("Labeling Rule 2: " + label.size());
		System.out.println("Labels: " + label.toString());

		try
		{
			this.label = label;
			System.out.println("After addAll()");
		} catch (Exception e)
		{
			System.out.println("Exception e: " + e);
		}
		System.out.println("Labeling Rule 3: " + label.size());
	}
}
