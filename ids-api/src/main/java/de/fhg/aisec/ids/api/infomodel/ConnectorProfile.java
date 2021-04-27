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
package de.fhg.aisec.ids.api.infomodel;

import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.TypedLiteral;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public final class ConnectorProfile implements Serializable {

  private static final long serialVersionUID = 2L;

  private SecurityProfile securityProfile;
  private URI connectorUrl;
  private URI maintainerUrl;
  private List<TypedLiteral> connectorEntityNames;

  public ConnectorProfile() {
    this.securityProfile = SecurityProfile.TRUST_SECURITY_PROFILE;
    this.connectorUrl = null;
    this.maintainerUrl = null;
    this.connectorEntityNames = null;
  }

  public ConnectorProfile(
      SecurityProfile profile,
      URI connectorUrl,
      URI maintainerUrl,
      List<TypedLiteral> connectorEntityNames) {
    super();
    this.securityProfile = profile;
    this.connectorUrl = connectorUrl;
    this.maintainerUrl = maintainerUrl;
    this.connectorEntityNames = connectorEntityNames;
  }

  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  public URI getConnectorUrl() {
    return connectorUrl;
  }

  public URI getMaintainerUrl() {
    return maintainerUrl;
  }

  public List<TypedLiteral> getConnectorEntityNames() {
    return connectorEntityNames;
  }

  public void setSecurityProfile(SecurityProfile securityProfile) {
    this.securityProfile = securityProfile;
  }

  public void setConnectorUrl(URI connectorUrl) {
    this.connectorUrl = connectorUrl;
  }

  public void setMaintainerUrl(URI maintainerUrl) {
    this.maintainerUrl = maintainerUrl;
  }

  public void setConnectorEntityNames(List<TypedLiteral> connectorEntityNames) {
    this.connectorEntityNames = connectorEntityNames;
  }
}
