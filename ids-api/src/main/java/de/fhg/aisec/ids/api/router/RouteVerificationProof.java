package de.fhg.aisec.ids.api.router;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a proof that a route is valid under a policy, i.e. that the policy will never violate the policy.
 * 
 * If the route can violate the policy, a set of counterexamples is given.
 * 
 * The set is not necessarily complete and contains message paths which are valid in term of the route, but violate the policy.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class RouteVerificationProof {
	private String routeId;
	private long proofTimeNanos;
	private boolean isValid = true;
	private List<CounterExample> counterexamples = new ArrayList<>();
	private String query = "";
	private String explanation = "";
	
	public RouteVerificationProof(String routeId) {
		if (routeId==null) {
			throw new NullPointerException("Null routeId");
		}
		this.routeId = routeId;
	}

	public String getRouteId() {
		return routeId;
	}

	public long getProofTimeNanos() {
		return proofTimeNanos;
	}


	public void setProofTimeNanos(long proofTimeNanos) {
		this.proofTimeNanos = proofTimeNanos;
	}


	public boolean isValid() {
		return isValid;
	}


	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}


	public List<CounterExample> getCounterexamples() {
		return counterexamples;
	}


	public void setCounterexamples(List<CounterExample> counterexamples) {
		this.counterexamples = counterexamples;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
	public String getQuery() {
		return this.query;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Proof for " + this.query + "\n");
		sb.append("returns " + this.isValid + "\n");
		sb.append("because " + this.explanation + "\n");
		sb.append("Example flows violating policy:\n");
		for (CounterExample ce : this.counterexamples) {
			sb.append("|-- " + ce.toString() + "\n\n");
		}
		return sb.toString();		
	}

	public void setExplanation(String explanation) {
		this.explanation = explanation;		
	}
	
	public String getExplanation() {
		return this.explanation;
	}
}

