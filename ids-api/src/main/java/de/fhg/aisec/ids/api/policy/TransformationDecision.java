/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform API
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
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
		if (labelsToAdd == null) {
			labelsToAdd = new HashSet<>();
		}
		if (labelsToRemove == null) {
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
