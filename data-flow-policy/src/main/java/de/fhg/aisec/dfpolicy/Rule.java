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

		try
		{
			label.clear();
			label.addAll(newLabels);
		} catch (Exception e)
		{
			System.out.println("Exception e: " + e);
		}
	}
}
