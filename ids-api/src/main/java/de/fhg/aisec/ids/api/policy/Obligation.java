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
package de.fhg.aisec.ids.api.policy;

public class Obligation {
  private String action;
  private PolicyDecision.Decision alternativeDecision;

  public Obligation() {
    super();
  }

  public Obligation(String action, PolicyDecision.Decision alternativeDecision) {
    this.action = action;
    this.alternativeDecision = alternativeDecision;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public PolicyDecision.Decision getAlternativeDecision() {
    return alternativeDecision;
  }

  public void setAlternativeDecision(PolicyDecision.Decision alternativeDecision) {
    this.alternativeDecision = alternativeDecision;
  }
}
