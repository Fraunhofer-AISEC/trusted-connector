package de.fhg.aisec.ids.api.policy;

/**
 * Bean representing the decision of a Policy Decision Point (PDP).
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class PolicyDecision {
	public enum Decision {
		ALLOW, DENY, DON_T_CARE
	}

	private String reason;
	private Decision decision;
	private Obligation obligation;
	
	public String getReason() {
		return reason;
	}
	
	public Obligation getObligation() {
		return obligation;
	}
	
	public void setObligation(Obligation obligation) {
		this.obligation = obligation;
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