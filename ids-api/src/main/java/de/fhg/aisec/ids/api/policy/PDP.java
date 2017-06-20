package de.fhg.aisec.ids.api.policy;

/**
 * Policy Decision Point (PDP) Interface.
 * 
 * The PDP decides decision requests against a policy. It may use caching to
 * speed up the decision.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public interface PDP {

	/**
	 * Main method for requesting a policy decision.
	 * 
	 * The decision request states attributes of subject and resource. The
	 * result is a decision that is expected to be enforced by the PEP.
	 * 
	 * @param req
	 * @return
	 */
	public PolicyDecision requestDecision(DecisionRequest req);

	/**
	 * Removes all data from PDP-internal caches. Future decisions will possibly
	 * take more time.
	 */
	public void clearAllCaches();
}
