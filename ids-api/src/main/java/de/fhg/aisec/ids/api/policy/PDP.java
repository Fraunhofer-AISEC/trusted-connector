package de.fhg.aisec.ids.api.policy;

/**
 * Policy Decision Point Interface.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface PDP {
	
	public PolicyDecision requestDecision(DecisionRequest req);
	
}
