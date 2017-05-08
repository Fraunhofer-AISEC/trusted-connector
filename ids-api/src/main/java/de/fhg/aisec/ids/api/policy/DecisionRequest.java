package de.fhg.aisec.ids.api.policy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data structure holding a decision request which is sent to the PDP.
 * The PDP is expected to answer with a PolicyDecision object.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class DecisionRequest {
	private String from;
	private String to;
	private Set<String> ctx = new HashSet<String>();
	
	public DecisionRequest(String from, String to, Set<String> ctx) {
		super();
		this.from = from;
		this.to = to;
		this.ctx = ctx;
	}
	
	/**
	 * Returns the source, i.e. the origin of the communication for which a decision is requested.
	 * @return
	 */
	public String getFrom() {
		return from;
	}
	
	/**
	 * Sets the source, i.e. the origin of the communication for which a decision is requested.
	 * @return
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * Returns the sink, i.e. the endpoint of the communication for which a decision is requested.
	 * @return
	 */
	public String getTo() {
		return to;
	}

	/**
	 * Sets the source, i.e. the origin of the communication for which a decision is requested.
	 * @return
	 */
	public void setTo(String to) {
		this.to = to;
	}
	
	/**
	 * A decision context may hold additional information which is passed at attributes to the PDP.
	 * The context may include 
	 * - a reference to previously taken decisions for the sake of caching
	 * - a reason for the request
	 * - timestamps
	 * - etc.
	 * @return
	 */
	public Set<String> getCtx() {
		return ctx;
	}
}
