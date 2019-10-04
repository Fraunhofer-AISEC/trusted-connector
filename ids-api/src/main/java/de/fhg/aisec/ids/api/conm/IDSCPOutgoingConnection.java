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
package de.fhg.aisec.ids.api.conm;

/**
 * Bean representing an "IDSCP Connection" .
 *
 * @author Gerd Brost (gerd.brost@aisec.fraunhofer.de)
 */
public class IDSCPOutgoingConnection {
  private String endpointIdentifier;
  private String remoteAuthentication;
  private String remoteIdentity;
  private String endpointKey;
  private RatResult attestationResult;
  private String metaData;

  public String getEndpointKey() {
    return endpointKey;
  }

  public void setEndpointKey(String endpointKey) {
    this.endpointKey = endpointKey;
  }

  public String getRemoteAuthentication() {
    return remoteAuthentication;
  }

  public void setRemoteAuthentication(String state) {
    this.remoteAuthentication = state;
  }

  public String getRemoteIdentity() {
    return remoteIdentity;
  }

  public void setRemoteIdentity(String hostname) {
    this.remoteIdentity = hostname;
  }

  public String getEndpointIdentifier() {
    return endpointIdentifier;
  }

  public void setEndpointIdentifier(String endpointIdentifier) {
    this.endpointIdentifier = endpointIdentifier;
  }

  public RatResult getAttestationResult() {
    return this.attestationResult;
  }

  public void setAttestationResult(RatResult attestationResult) {
    this.attestationResult = attestationResult;
  }

  @Override
  public String toString() {
    return "IDSCPOutgoingConnection [endpoint_identifier=" + endpointIdentifier + "]";
  }

  public void setMetaData(String metaResult) {
    this.metaData = metaResult;
  }

  public String getMetaData() {
    return this.metaData;
  }
}
