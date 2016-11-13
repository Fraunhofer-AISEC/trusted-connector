package de.fhg.aisec.dfpolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Rule {
	private Set<String> label = new HashSet<>();

	public Set<String> getLabel() {
		return label;
	}

	public void setLabel(List<String> labels) {
		label.clear();
		label.addAll(labels);
	}
}
