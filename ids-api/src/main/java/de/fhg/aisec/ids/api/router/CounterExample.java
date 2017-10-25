package de.fhg.aisec.ids.api.router;

import java.util.ArrayList;
import java.util.List;

public class CounterExample {
	private List<String> steps = new ArrayList<>();
	
	public void addStep(String s) {
		if (s!=null) {
			steps.add(s);
		}
	}
	
	public List<String> getSteps() {
		return steps;
	}
	
	@Override
	public String toString() {
		return String.join("\n|-- ", steps);
	}
}
