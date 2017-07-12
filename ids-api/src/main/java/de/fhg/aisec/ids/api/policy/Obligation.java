package de.fhg.aisec.ids.api.policy;

public class Obligation {
	private String action;
	private PolicyDecision.Decision alternativeDecision;
	
	public Obligation() {
		super();
	}
	
	public Obligation(String action, PolicyDecision.Decision alternativeDecision) {
		this.action = action;
		this.alternativeDecision = alternativeDecision;
	}
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public PolicyDecision.Decision getAlternativeDecision() {
		return alternativeDecision;
	}
	
	public void setAlternativeDecision(PolicyDecision.Decision alternativeDecision) {
		this.alternativeDecision = alternativeDecision;
	}
}
