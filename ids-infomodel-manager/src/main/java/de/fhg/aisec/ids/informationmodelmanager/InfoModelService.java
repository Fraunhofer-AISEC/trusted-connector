/*-
 * ========================LICENSE_START=================================
 * InfoModelService
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
/*
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
 */
package de.fhg.aisec.ids.informationmodelmanager;

import com.google.gson.Gson;

import de.fhg.aisec.ids.api.ConnectionSettings;
import de.fhg.aisec.ids.api.conm.ConnectionManager;
import de.fhg.aisec.ids.api.conm.IDSCPServerEndpoint;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//import org.apache.commons.lang3.ObjectUtils;

import de.fraunhofer.iais.eis.util.VocabUtil;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * IDS Info Model Manager.
 *
 */
@Component(enabled=true, immediate=true, name = "ids-infomodel-manager")
public class InfoModelService implements InfoModel {
	private static final Logger LOG = LoggerFactory.getLogger(InfoModelService.class);
	private static final String CONNECTOR_MODEL = "ids.model";
	private PreferencesService preferencesService = null;
	private ConnectionManager connectionManager = null;
	
	Preferences p;

	@Activate
	protected void activate() {
		LOG.info("Activating Info Model Manager");
	}

	@Deactivate
	protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
		LOG.info("Deactivating Info Model Manager");
	}
	
	@Reference(name = "config.service",
			service = PreferencesService.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConfigurationService")
	public void bindConfigurationService(PreferencesService conf) {
		LOG.info("Bound to configuration service");
		preferencesService = conf;
		//TODO: Do we need to do any housekeeping?
	}

	public void unbindConfigurationService(PreferencesService conf) {
		preferencesService = null;
	}
	
	@Reference(name = "connections.service",
            service = ConnectionManager.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindConnectionManager")
    protected void bindConnectionManager(ConnectionManager conn) {
        LOG.info("Bound to connection manager");
        connectionManager = conn;
    }

    protected void unbindConnectionManager(ConnectionManager conn) {
        connectionManager = null;
    }

	private void setPreference(String key, String value, Preferences p) {
		if (p != null) {
			p.put(key, value);
		} else {
			LOG.error("No Preferences available.");
		}
	}
	
	private URL getURL(String key) {
		URL url;

		if (preferencesService!=null) {
			
			if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
				try {
					url = new URL(p.get(key, null));
					return url;
				} catch (MalformedURLException ex) {
					//LOG.debug("Caught MalformedURLException while building URL from preferences", ex);
					LOG.debug("Caught MalformedURLException while building URL from preferences");
					return null;
				}
			} else {
				LOG.error("No Preferences available for building URL.");
				return null;
			}
		}
		//TODO: What is good behaviour here?
		return null;
	}

	// Build Connector Entity Names
	private List<PlainLiteral> getConnectorEntityNames() {
		
		if (preferencesService!=null) {
			
	
			if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
				return new Gson().fromJson(p.get("conn_entity", null), ArrayList.class);
			} else {
				LOG.error("Couldn't get Connector Entity Names");
				return null;
			}
		}
		return null;
	}

	// Build Endpoints
	private List<DataEndpoint> getEndpoints() {

		DataEndpoint eP;
		List<DataEndpoint> ePs = new ArrayList<>();
		
		if (connectionManager!=null) {
		
			List<IDSCPServerEndpoint> sePs = connectionManager.listAvailableEndpoints();
	
			for (IDSCPServerEndpoint tempEP : sePs) {
				// TODO build EndpointIdentifier like URL in Infomodell
				// http://industrialdataspace.org/connector1/endpoint1
				try {
					eP = new DataEndpointBuilder()
							.entityNames(Arrays.asList(new PlainLiteral(tempEP.getEndpointIdentifier()))).build();
					ePs.add(eP);
				} catch (ConstraintViolationException ex) {
					LOG.error("Caught ConstraintViolationException while building Endpoints", ex);
				}
			}
	
			return ePs;
		}
		return null;
	}

	// Build Security Profile
	private SecurityProfile getSecurityProfile() {

		if (preferencesService!=null) {
	
	
			if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
				try {
					PredefinedSecurityProfile psp;
					if(!p.get("basedOn", "").equals(""))
						psp =  PredefinedSecurityProfile.getByString(p.get("basedOn", ""));
					else
						psp = null;

					if(!p.get("sPiD", "").equals(""))
						return new SecurityProfileBuilder(new URL(p.get("sPiD", ""))).basedOn(psp)
							.integrityProtectionAndVerification(IntegrityProtectionAndVerification
									.valueOf(p.get("IntegrityProtectionAndVerification", "NONE")))
							.authenticationSupport(AuthenticationSupport.valueOf(p.get("AuthenticationSupport", "NONE")))
							.serviceIsolationSupport(
									ServiceIsolationSupport.valueOf(p.get("ServiceIsolationSupport", "NONE")))
							.integrityProtectionScope(
									IntegrityProtectionScope.valueOf(p.get("IntegrityProtectionScope", "NONE")))
							.appExecutionResources(AppExecutionResources.valueOf(p.get("AppExecutionResources", "NONE")))
							.dataUsageControlSupport(
									DataUsageControlSupport.valueOf(p.get("DataUsageControlSupport", "NONE")))
							.auditLogging(AuditLogging.valueOf(p.get("AuditLogging", "NONE")))
							.localDataConfidentiality(
									idsLocalDataConfidentiality.valueOf(p.get("idsLocalDataConfidentiality", "NONE")))
							.build();
					else
						return new SecurityProfileBuilder().basedOn(psp)			//id automatically generated
								.integrityProtectionAndVerification(IntegrityProtectionAndVerification
										.valueOf(p.get("IntegrityProtectionAndVerification", "NONE")))
								.authenticationSupport(AuthenticationSupport.valueOf(p.get("AuthenticationSupport", "NONE")))
								.serviceIsolationSupport(
										ServiceIsolationSupport.valueOf(p.get("ServiceIsolationSupport", "NONE")))
								.integrityProtectionScope(
										IntegrityProtectionScope.valueOf(p.get("IntegrityProtectionScope", "NONE")))
								.appExecutionResources(AppExecutionResources.valueOf(p.get("AppExecutionResources", "NONE")))
								.dataUsageControlSupport(
										DataUsageControlSupport.valueOf(p.get("DataUsageControlSupport", "NONE")))
								.auditLogging(AuditLogging.valueOf(p.get("AuditLogging", "NONE")))
								.localDataConfidentiality(
										idsLocalDataConfidentiality.valueOf(p.get("idsLocalDataConfidentiality", "NONE")))
								.build();
				} catch (ConstraintViolationException ex) {
					LOG.error("Caught ConstraintViolationException while building Security profile from preferences.", ex);
					return null;
				} catch (MalformedURLException ex) {
				LOG.error("Caught MalformedURLException while building Security profile from preferences.", ex);
				return null;
			}
			} else {
				LOG.error("Couldn't get Preferences for building Security Profile.");
				return null;
			}
		}
		return null;
	}

	private boolean generateRDF(Connector c) {

		if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
			String rdf = c.toRdf();
			p.put("infomodel", rdf);
			LOG.debug("Generated RDF description: " + rdf);
			return true;
		} else {
			LOG.debug("Couldn't get Preferences for generating RDF.");
			return false;
		}
	}

	@Override
	public String getRDF() {
		if (preferencesService!=null) {

			if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
				return p.get("infomodel", "");
			} else {
				LOG.error("Couldn't get Connector description.");
				return null;
			}
		}
		return null;
	}

	@Override
	public Connector getConnector() {

		Connector c = null;
		SecurityProfile securityprofile = getSecurityProfile();
		List<DataEndpoint> endpoints = getEndpoints();
		URL conn_url = getURL("conn_url");
		URL op_url = getURL("op_url");
		List<PlainLiteral> entityNames = getConnectorEntityNames();

		if(!((op_url==null) || (entityNames==null))) {
			try {
				if(conn_url!=null)
					return new ConnectorBuilder(conn_url).operator(op_url).entityNames(entityNames)
						.securityProfile(securityprofile).provides(endpoints).build();
				//LOG.info("Successfully build connector.");
				else
					return new ConnectorBuilder().operator(op_url).entityNames(entityNames)
							.securityProfile(securityprofile).provides(endpoints).build();
				
			} catch (ConstraintViolationException ex) {
				LOG.error("Caught ConstraintViolationException while building Connector", ex);
				return null;
			}
		} else {
			//LOG.debug("Connector couldn't be built due to null objects.");
			return null;
		}
	}

	@Override
	public boolean setConnector(URL conn_url, URL op_url, Collection<PlainLiteral> entityNames,
			SecurityProfile profile) {

		if (preferencesService!=null) {
	
			if ((p = preferencesService.getUserPreferences(CONNECTOR_MODEL)) != null) {
	
				// Set Connector URL ---> allowed to be empty from model
				if (conn_url != null) {
					setPreference("conn_url", conn_url.toString(), p);
				} /*else {
					//setPreference("conn_url", "someNonRandomString", p);
					setPreference("conn_url", VocabUtil.getInstance().createRandomUrl("connector").toString(), p);
					//TODO: Check instantiation of VocabUtil.createRandomUrl("connector").toString()
				}*/
	
				// Set Operator URL
				if (op_url != null) {
					setPreference("op_url", op_url.toString(), p);
				} else {
					LOG.error("Operator URL can not be empty.");
					return false;
				}
	
				// Set Entity Names
				if(entityNames!=null && !entityNames.isEmpty()) {
					setPreference("conn_entity", new Gson().toJson(entityNames), p);
				} else {
					LOG.error("Entity Names can not be empty.");
					return false;
				}
	
				// Set SecurityProfile -- if null, will return "NONE" automatically
				if (profile != null) {
					if(profile.getBasedOn()!=null)
						setPreference("basedOn", profile.getBasedOn().toString(), p);
					setPreference("sPiD", profile.getId().toString(), p);
					setPreference("IntegrityProtectionAndVerification",
							profile.getIntegrityProtectionAndVerification().toString(), p);
					setPreference("AuthenticationSupport", profile.getAuthenticationSupport().toString(), p);
					setPreference("ServiceIsolationSupport", profile.getServiceIsolationSupport().toString(), p);
					setPreference("IntegrityProtectionScope", profile.getIntegrityProtectionScope().toString(), p);
					setPreference("AppExecutionResources", profile.getAppExecutionResources().toString(), p);
					setPreference("DataUsageControlSupport", profile.getDataUsageControlSupport().toString(), p);
					setPreference("AuditLogging", profile.getAuditLogging().toString(), p);
					setPreference("idsLocalDataConfidentiality", profile.getLocalDataConfidentiality().toString(), p);
				}

				try {
					if(conn_url!=null)
						return generateRDF(new ConnectorBuilder(conn_url).operator(op_url).entityNames(entityNames)
								.securityProfile(profile).build());
					else
						return generateRDF(new ConnectorBuilder().operator(op_url).entityNames(entityNames)
								.securityProfile(profile).build());
				} catch (ConstraintViolationException ex) {
					LOG.error("Caught ConstraintViolationException while building Connector to generate RDF description.", ex);
					return false;
				}

			} else {
				LOG.error("Couldn't set Preferences.");
				return false;
			}
		} else {
			LOG.debug("Couldn't load preferences to store connector object.");
			return false;
		}
	}

	@Override
	public boolean setConnector(URL conn_url, URL op_url, Collection<PlainLiteral> entityNames) {
		return setConnector(conn_url, op_url, entityNames, null);
	}

	@Override
	public boolean setConnector(URL op_url, Collection<PlainLiteral> entityNames) {
		return setConnector(null, op_url, entityNames, null);
	}

	/*
	 * @Override public boolean setConnector(Connector c){
	 * 
	 * //Set Connector URL setConnectorURL(c.getId().toString());
	 * 
	 * //Set Operator URL setOperatorURL(c.getOperator().toString());
	 * 
	 * //Set Entity Name setConnectorEntityNames(c.getEntityNames());
	 * 
	 * //Get SecurityProfile -- if null will return "NONE" automatically
	 * if(c.getSecurityProfile()!=null){
	 * setSecurityProfile(c.getSecurityProfile()); }
	 * }
	 */

}
