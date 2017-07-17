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

/**
 * Metrics of a single route.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class RouteMetrics {
	private long completed;
	private long failed;
	private long inflight;
	private long failuresHandled;
	private long redeliveries;
	private long maxProcessingTime;
	private long meanProcessingTime;
	private long minProcessingTime;
	
	public long getCompleted() {
		return completed;
	}
	public long getFailed() {
		return failed;
	}
	public long getInflight() {
		return inflight;
	}
	public long getFailuresHandled() {
		return failuresHandled;
	}
	public long getRedeliveries() {
		return redeliveries;
	}
	public long getMaxProcessingTime() {
		return maxProcessingTime;
	}
	public long getMeanProcessingTime() {
		return meanProcessingTime;
	}
	public long getMinProcessingTime() {
		return minProcessingTime;
	}
	public void setCompleted(long completed) {
		this.completed = completed;
	}
	public void setFailed(long failed) {
		this.failed = failed;
	}
	public void setInflight(long inflight) {
		this.inflight = inflight;
	}
	public void setFailuresHandled(long failuresHandled) {
		this.failuresHandled = failuresHandled;
	}
	public void setRedeliveries(long redeliveries) {
		this.redeliveries = redeliveries;
	}
	public void setMaxProcessingTime(long maxProcessingTime) {
		this.maxProcessingTime = maxProcessingTime;
	}
	public void setMeanProcessingTime(long meanProcessingTime) {
		this.meanProcessingTime = meanProcessingTime;
	}
	public void setMinProcessingTime(long minProcessingTime) {
		this.minProcessingTime = minProcessingTime;
	}
}
