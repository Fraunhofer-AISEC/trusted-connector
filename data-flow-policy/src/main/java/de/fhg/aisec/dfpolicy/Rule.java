/*-
 * ========================LICENSE_START=================================
 * Data Flow Policy
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
package de.fhg.aisec.dfpolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Rule {
	private Set<String> label = new HashSet<>();

	public Set<String> getLabel() {
		return label;
	}

	public void setLabel(List<String> labels) {
		label.clear();
		label.addAll(labels);
	}
}
