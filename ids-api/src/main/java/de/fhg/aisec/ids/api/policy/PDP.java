package de.fhg.aisec.ids.api.policy;

/**
 * Policy Decision Point (PDP) Interface.
 * 
 * The PDP decides decision requests against a policy. It may use caching to speed 
 * up the decision.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface PDP {

	public PolicyDecision requestDecision(DecisionRequest req);

	/**
	 * Removes all data from PDP-internal caches. Future decisions will possibly
	 * take more time.
	 */
	public void clearAllCaches();

}
