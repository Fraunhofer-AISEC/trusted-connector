/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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
package de.fhg.ids.comm.ws.protocol.fsm;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a state with some number of associated transitions.
 */
class State {
	// map from event key to transition
	Map<Object, Transition> transitions = new HashMap<>();
	Runnable entryCode;
	Runnable exitCode;
	Runnable alwaysRunCode;

	State(Runnable entryCode, Runnable exitCode, Runnable alwaysRunCode) {
		this.entryCode = entryCode;
		this.exitCode = exitCode;
		this.alwaysRunCode = alwaysRunCode;
	}

	public void addTransition(Transition trans) {
		// Fail fast for duplicate transitions
		if (transitions.containsKey(trans.event)) {
			throw new IllegalArgumentException("Transition for event " + trans.event + " already exists.");
		}
		transitions.put(trans.event, trans);
	}

	public void runEntryCode() {
		if (entryCode != null) {
			entryCode.run();
		}
	}

	public void runExitCode() {
		if (exitCode != null) {
			exitCode.run();
		}
	}

	public void runAlwaysCode() {
		if (alwaysRunCode != null) {
			alwaysRunCode.run();
		}
	}
}
