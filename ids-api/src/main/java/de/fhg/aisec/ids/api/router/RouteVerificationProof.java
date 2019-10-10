/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
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

/**
 * Representation of a proof that a route is valid under a policy, i.e. that the policy will never
 * violate the policy.
 *
 * <p>If the route can violate the policy, a set of counterExamples is given.
 *
 * <p>The set is not necessarily complete and contains message paths which are valid in term of the
 * route, but violate the policy.
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class RouteVerificationProof {
  private String routeId;
  private long proofTimeNanos;
  private boolean isValid = true;
  private List<CounterExample> counterExamples = new ArrayList<>();
  private String query = "";

  public RouteVerificationProof(String routeId) {
    if (routeId == null) {
      throw new NullPointerException("routeId must not be null");
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

  public List<CounterExample> getCounterExamples() {
    return counterExamples;
  }

  public void setCounterExamples(List<CounterExample> counterExamples) {
    this.counterExamples = counterExamples;
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
    sb.append("Proof for ").append(this.query).append("\n");
    sb.append("returns ").append(this.isValid).append("\n");
    sb.append("Example flows violating policy:\n");
    for (CounterExample ce : this.counterExamples) {
      sb.append("|-- ").append(ce.toString()).append("\n\n");
    }
    return sb.toString();
  }
}
