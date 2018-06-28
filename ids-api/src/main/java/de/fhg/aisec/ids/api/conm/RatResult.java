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
package de.fhg.aisec.ids.api.conm;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Result of a remote attestation between peers.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */

public class RatResult {
	public enum Status {
		FAILED,
		SUCCESS
	}

	private @NonNull Status status;
	private @Nullable String reason;
	
	public RatResult(@NonNull Status status, @Nullable String reason) {
		this.status = status;
		if (reason != null) {
			this.reason = reason;
		} else {
			this.reason = "";
		}
	}

	public Status getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}
}


