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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.util.PlainLiteral;

//@JsonDeserialize(using = ProductDeserializer.class)
public final class ConnectorProfile implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	//Security Profile settings
	private final SecurityProfile securityProfile;
	//private final String integrityProtectionAndVerification;		//"NONE", "LOCAL_INTEGRITY_PROTECTION", "REMOTE_INTEGRITY_VERIFICATION"  
	//private final String authenticationSupport;						//"NONE", "SERVER_SIDE_ONLY", "MUTUAL"
	//private final String serviceIsolation;							//"NONE", "PROCESS_GROUP_ISOLATION", "LEAST_PRIVILEGE_BASED_ISOLATION"
	//private final String integrityProtectionVerificationScope;		//"NONE", "KERNEL_AND_CORE_CONTAINER", "KERNEL_AND_CORE_CONTAINER_AND_APP_CONTAINERS"
	//private final String appExecutionResources;						//"NONE", "LOCAL_ENFORCEMENT", "REMOTE_VERIFICATION"
	//private final String dataUsageControlSupport;					//"NONE", "USAGE_CONTROL_POLICY_ENFORCEMENT", "REMOTE_POLICY_COMPLIANCE_VERIFICATION"
	//private final String auditLogging;								//"NONE", "LOCAL_LOGGING_AND_INTEGRITY_PROTECTION", "REMOTE_AUDIT_LOG_TRACING"
	//private final String localDataConfidentiality;					//"NONE", "SECURE_DATA_ERASURE", "LOCAL_FULL_DATA_ENCRYPTION"
	private final String connectorURL;
	private final String operatorURL;
	private final Collection<PlainLiteral> connectorEntityNames;
	//private final String connectorEntityName;
	
	public ConnectorProfile() {
		this.securityProfile = null;
		this.connectorURL = "";
		this.operatorURL = "";
		this.connectorEntityNames = Arrays.asList(new PlainLiteral(""));
	}
	
	public ConnectorProfile(SecurityProfile profile, String connectorURL, String operatorURL,
			  Collection<PlainLiteral> connectorEntityNames) {
	super();
	this.securityProfile = profile;
	this.connectorURL = connectorURL;
	this.operatorURL = operatorURL;
	this.connectorEntityNames = connectorEntityNames;
	}

	public SecurityProfile getSecurityProfile(){
		return securityProfile;
	}
	
	public String getConnectorURL(){
		return connectorURL;
	}
	
	public String getOperatorURL(){
		return operatorURL;
	}
	
	public Collection<PlainLiteral> getConnectorEntityNames(){
		return connectorEntityNames;
	}
	
}

