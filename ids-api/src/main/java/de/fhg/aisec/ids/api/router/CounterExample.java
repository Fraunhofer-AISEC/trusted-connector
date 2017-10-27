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
package de.fhg.aisec.ids.api.router;

import java.util.ArrayList;
import java.util.List;

public class CounterExample {
	private List<String> steps = new ArrayList<>();
	
	public void addStep(String s) {
		if (s!=null) {
			steps.add(s);
		}
	}
	
	public List<String> getSteps() {
		return steps;
	}
	
	@Override
	public String toString() {
		return String.join("\n|-- ", steps);
	}
}
