package de.fhg.aisec.ids.api.policy;

import java.util.HashMap;
import java.util.Map;

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
	private Map<String, String> msgCtx = new HashMap<>();
	private Map<String, String> envCtx = new HashMap<>();
	
	public DecisionRequest(String from, String to, Map<String, String> msgCtx, Map<String, String> envCtx) {
		super();
		this.from = from;
		this.to = to;
		this.msgCtx = msgCtx;
		this.envCtx = envCtx;
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
	 * A decision context may hold additional information about the message/event.
	 * It is passed as attributes to the PDP.
	 * 
	 * The context may include 
	 * - timestamps
	 * - route ids
	 * - etc.
	 * @return
	 */
	public Map<String, String> getMessageCtx() {
		return msgCtx;
	}

	/**
	 * A decision context may hold additional information about the overall 
	 * system environment of the PEP..
	 * It is passed as attributes to the PDP.
	 * 
	 * The context may include 
	 * - a reference to previously taken decisions for the sake of caching
	 * - a reason for the request
	 * - identifiers of available components
	 * - etc.
	 * @return
	 */
	public Map<String, String> getEnvironmentCtx() {
		return envCtx;
	}
}
