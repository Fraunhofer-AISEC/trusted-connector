package de.fhg.aisec.ids.api.policy;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the result of a transformation function as returned by
 * the policy decision point (PDP).
 * 
 * The TransformationDecision defines labels which must be added to or 
 * removed from a message.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class TransformationDecision {
	private Set<String> labelsToAdd;
	private Set<String> labelsToRemove;
	
	public TransformationDecision() {
		this.labelsToAdd = new HashSet<>();
		this.labelsToRemove = new HashSet<>();
	}
	
	public TransformationDecision(Set<String> labelsToAdd, Set<String> labelsToRemove) {
		super();
		if (labelsToAdd==null) {
			labelsToAdd = new HashSet<>();
		}
		if (labelsToRemove==null) {
			labelsToRemove = new HashSet<>();
		}
		this.labelsToAdd = labelsToAdd;
		this.labelsToRemove = labelsToRemove;
	}

	/**
	 * Returns a (possibly empty, but never null) set of labels that must be
	 * added to a message.
	 * 
	 * @return
	 */
	public Set<String> getLabelsToAdd() {
		return labelsToAdd;
	}

	/**
	 * Returns a (possibly empty, but never null) set of labels that must be
	 * removed from a message.
	 * 
	 * @return
	 */
	public Set<String> getLabelsToRemove() {
		return labelsToRemove;
	}
}