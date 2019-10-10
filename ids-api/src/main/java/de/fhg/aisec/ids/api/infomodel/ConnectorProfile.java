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

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

public final class ConnectorProfile implements Serializable {

  private static final long serialVersionUID = 1L;

  private final SecurityProfile securityProfile;
  private final URL connectorUrl;
  private final URL maintainerUrl;
  private final List<PlainLiteral> connectorEntityNames;

  public ConnectorProfile() {
    this.securityProfile =
        new SecurityProfileBuilder()
            ._basedOn_(PredefinedSecurityProfile.LEVEL0SECURITYPROFILE)
            ._appExecutionResources_(AppExecutionResources.NO_APP_EXECUTION)
            ._auditLogging_(AuditLogging.NO_AUDIT_LOGGING)
            ._authenticationSupport_(AuthenticationSupport.NO_AUTHENTICATION)
            ._dataUsageControlSupport_(DataUsageControlSupport.NO_USAGE_CONTROL)
            ._integrityProtectionAndVerificationSupport_(
                IntegrityProtectionAndVerificationSupport.NO_INTEGRITY_PROTECTION)
            ._integrityProtectionScope_(IntegrityProtectionScope.UNKNOWN_INTEGRITY_PROTECTION_SCOPE)
            ._localDataConfidentiality_(LocalDataConfidentiality.NO_CONFIDENTIALITY)
            ._serviceIsolationSupport_(ServiceIsolationSupport.NO_SERVICE_ISOLATION)
            .build();
    this.connectorUrl = null;
    this.maintainerUrl = null;
    this.connectorEntityNames = null;
  }

  public ConnectorProfile(
      SecurityProfile profile,
      URL connectorUrl,
      URL maintainerUrl,
      List<PlainLiteral> connectorEntityNames) {
    super();
    this.securityProfile = profile;
    this.connectorUrl = connectorUrl;
    this.maintainerUrl = maintainerUrl;
    this.connectorEntityNames = connectorEntityNames;
  }

  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  public URL getConnectorUrl() {
    return connectorUrl;
  }

  public URL getMaintainerUrl() {
    return maintainerUrl;
  }

  public List<PlainLiteral> getConnectorEntityNames() {
    return connectorEntityNames;
  }
}
