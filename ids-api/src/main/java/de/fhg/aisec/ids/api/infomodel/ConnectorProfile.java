/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform API
 * %%
 * Copyright (C) 2017 - 2018 Fraunhofer AISEC
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
import de.fraunhofer.iais.eis.util.PlainLiteral;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

public final class ConnectorProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SecurityProfile securityProfile;
    private final URL connectorURL;
    private final URL operatorURL;
    private final List<PlainLiteral> connectorEntityNames;

    public ConnectorProfile() {
        this.securityProfile = null;
        this.connectorURL = null;
        this.operatorURL = null;
        this.connectorEntityNames = null;
    }

    public ConnectorProfile(SecurityProfile profile, URL connectorURL, URL operatorURL,
                            List<PlainLiteral> connectorEntityNames) {
        super();
        this.securityProfile = profile;
        this.connectorURL = connectorURL;
        this.operatorURL = operatorURL;
        this.connectorEntityNames = connectorEntityNames;
    }

    public SecurityProfile getSecurityProfile() {
        return securityProfile;
    }

    public URL getConnectorURL() {
        return connectorURL;
    }

    public URL getOperatorURL() {
        return operatorURL;
    }

    public List<PlainLiteral> getConnectorEntityNames() {
        return connectorEntityNames;
    }

}

