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
package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
public class IDSCPIncomingConnection {
  private String endpointIdentifier;
  private RatResult attestationResult;
  private String endpointKey;
  private String remoteHostName;
  private String metaData;

  public String getEndpointIdentifier() {
    return endpointIdentifier;
  }

  public void setEndpointIdentifier(String endpointIdentifier) {
    this.endpointIdentifier = endpointIdentifier;
  }

  public RatResult getAttestationResult() {
    return attestationResult;
  }

  public void setAttestationResult(RatResult result) {
    this.attestationResult = result;
  }

  @Override
  public String toString() {
    return "IDSCPConnection [endpoint_identifier="
        + endpointIdentifier
        + ", attestationResult="
        + attestationResult
        + "]";
  }

  public void setEndpointKey(String connectionKey) {
    this.endpointKey = connectionKey;
  }

  public String getEndpointKey() {
    return endpointKey;
  }

  public String getRemoteHostName() {
    return remoteHostName;
  }

  public void setRemoteHostName(String remoteHostname) {
    this.remoteHostName = remoteHostname;
  }

  public void setMetaData(String metaResult) {
    this.metaData = metaResult;
  }

  public String getMetaData() {
    return this.metaData;
  }
}
