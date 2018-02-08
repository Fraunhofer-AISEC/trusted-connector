/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
 * %%
 * Copyright (C) 2017 Fraunhofer AISEC
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
package de.fhg.aisec.ids.webconsole.connectionsettings;


public class ConnectionSettings {
	
	private String integrityProtectionAndVerification;
	private String authentication;
	private String serviceIsolation;
	private String integrityProtectionVerificationScope;
	private String appExecutionResources;
	private String dataUsageControlSupport;
	private String auditLogging;
	private String localDataConfidentiality;

	
	public ConnectionSettings(String integrityProtectionAndVerification, String authentication,
							  String serviceIsolation, String integrityProtectionVerificationScope, String appExecutionResources,
							  String dataUsageControlSupport, String auditLogging, String localDataConfidentiality) {
		super();
		this.integrityProtectionAndVerification = integrityProtectionAndVerification;
		this.authentication = authentication;
		this.serviceIsolation = serviceIsolation;
		this.integrityProtectionVerificationScope = integrityProtectionVerificationScope;
		this.appExecutionResources = appExecutionResources;
		this.dataUsageControlSupport = dataUsageControlSupport;
		this.auditLogging = auditLogging;
		this.localDataConfidentiality = localDataConfidentiality;
	}
	
	public ConnectionSettings(ConnectionSettings inConnConfiguration) {
		super();
		this.integrityProtectionAndVerification = inConnConfiguration.getIntegrityProtectionAndVerification();
		this.authentication = inConnConfiguration.getAuthentication();
		this.serviceIsolation = inConnConfiguration.getServiceIsolation();
		this.integrityProtectionVerificationScope = inConnConfiguration.getIntegrityProtectionVerificationScope();
		this.appExecutionResources = inConnConfiguration.getAppExecutionResources();;
		this.dataUsageControlSupport = inConnConfiguration.getDataUsageControlSupport();
		this.auditLogging = inConnConfiguration.getAuditLogging();
		this.localDataConfidentiality = inConnConfiguration.getLocalDataConfidentiality();
	}
	
	public ConnectionSettings() {
		this.integrityProtectionAndVerification = "1";
		this.authentication = "1";
		this.serviceIsolation = "1";
		this.integrityProtectionVerificationScope = "1";
		this.appExecutionResources = "1";
		this.dataUsageControlSupport = "1";
		this.auditLogging = "1";
		this.localDataConfidentiality = "1";
	}

	public String getIntegrityProtectionAndVerification() {
		return integrityProtectionAndVerification;
	}
	
	public void setIntegrityProtectionAndVerification(String integrityProtectionAndVerification) {
		this.integrityProtectionAndVerification = integrityProtectionAndVerification;
	}
	
	public String getAuthentication() {
		return authentication;
	}
	
	public void setAuthentication(String authentication) {
		this.authentication = authentication;
	}
	
	public String getServiceIsolation() {
		return serviceIsolation;
	}
	
	public void setServiceIsolation(String serviceIsolation) {
		this.serviceIsolation = serviceIsolation;
	}
	
	public String getIntegrityProtectionVerificationScope() {
		return integrityProtectionVerificationScope;
	}
	
	public void setIntegrityProtectionVerificationScope(String integrityProtectionVerificationScope) {
		this.integrityProtectionVerificationScope = integrityProtectionVerificationScope;
	}
	
	public String getAppExecutionResources() {
		return appExecutionResources;
	}
	
	public void setAppExecutionResources(String appExecutionResources) {
		this.appExecutionResources = appExecutionResources;
	}
	
	public String getDataUsageControlSupport() {
		return dataUsageControlSupport;
	}
	
	public void setDataUsageControlSupport(String dataUsageControlSupport) {
		this.dataUsageControlSupport = dataUsageControlSupport;
	}
	
	public String getAuditLogging() {
		return auditLogging;
	}
	
	public void setAuditLogging(String auditLogging) {
		this.auditLogging = auditLogging;
	}
	
	public String getLocalDataConfidentiality() {
		return localDataConfidentiality;
	}
	
	public void setLocalDataConfidentiality(String localDataConfidentiality) {
		this.localDataConfidentiality = localDataConfidentiality;
	}

}
