package de.fhg.aisec.ids.api.policy;

public class PolicyDecision {
	public enum Decision {
		ALLOW, DENY, DON_T_CARE
	}

	private String reason;
	private Decision decision;
	
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public Decision getDecision() {
		return decision;
	}
	public void setDecision(Decision decision) {
		this.decision = decision;
	}
}
