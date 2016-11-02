package de.fhg.aisec.dfpolicy;

import java.util.HashSet;
import java.util.Set;

public class Rule {
	
	private Set<String> label = new HashSet<String>();
	
	//TODO this.label.addALL(label) -> hängt nur an ggf. this.label.clear() davor machen.
	public Set<String> getLabel() {
		return label;
	}
	
	public void setLabel(Set<String> newLabels) {
		System.out.println("Labeling Rule 2: " + label.size());
		System.out.println("newLabels: " + newLabels.toString());
		System.out.println("label: " + label.toString());

		try
		{
			label.addAll(newLabels);
		} catch (Exception e)
		{
			System.out.println("Exception e: " + e);
		}
		System.out.println("Labeling Rule 3: " + label.size());
	}
}
