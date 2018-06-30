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
package de.fhg.aisec.ids.api.settings;

import java.io.Serializable;

public final class ConnectorConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	private String appstoreUrl = "https://raw.githubusercontent.com/portainer/templates/master/templates.json";
    private String brokerUrl = "";
    private String ttpHost = "";
    private int ttpPort = 443;
    private String acmeServerWebcon = "";
    private String acmeDnsWebcon = "";
    private int acmePortWebcon = 80;
    private boolean tosAcceptWebcon = false;

    public String getAppstoreUrl() {
    	return appstoreUrl;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getTtpHost() {
        return ttpHost;
    }

    public int getTtpPort() {
        return ttpPort;
    }

    public String getAcmeServerWebcon() {
        return acmeServerWebcon;
    }

    public String getAcmeDnsWebcon() {
        return acmeDnsWebcon;
    }

    public int getAcmePortWebcon() {
        return acmePortWebcon;
    }

    public boolean isTosAcceptWebcon() {
        return tosAcceptWebcon;
    }
}

