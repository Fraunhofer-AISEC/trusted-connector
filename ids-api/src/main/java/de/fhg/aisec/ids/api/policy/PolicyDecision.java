/*-
 * ========================LICENSE_START=================================
 * ids-api
 * %%
 * Copyright (C) 2018 Fraunhofer AISEC
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

import java.util.List;

/**
 * Bean representing the decision of a Policy Decision Point (PDP).
 *
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 */
public class PolicyDecision {
  public enum Decision {
    ALLOW,
    DENY,
    DON_T_CARE
  }

  private String reason;
  private Decision decision;
  private List<Obligation> obligations;

  public String getReason() {
    return reason;
  }

  public List<Obligation> getObligations() {
    return obligations;
  }

  public void setObligations(List<Obligation> obligation) {
    this.obligations = obligation;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Decision getDecision() {
    return decision;
  }

  public void setDecision(Decision decision) {
    this.decision = decision;
  }
}
