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

	/**
	 * Requests the PDP for the result of applying a transformation function to a message.
	 * 
	 * Transformation functions remove and/or add labels to messages. For non-flow-aware
	 * services, transformation functions are defined as part of the policy.
	 * 
	 * A transformation function must always applied to a message before the policy decision
	 * is requested using <code>requestDecision</code>.
	 * 
	 * @param lastServiceNode The last service the message exchange has been sent to.
	 * @return
	 */
	public TransformationDecision requestTranformations(ServiceNode lastServiceNode);
}
