/*-
 * ========================LICENSE_START=================================
 * IDS Core Platform Webconsole
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
package de.fhg.aisec.ids.webconsole.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.fhg.aisec.ids.api.ConnectionSettings;
import de.fhg.aisec.ids.api.Constants;
import de.fhg.aisec.ids.api.infomodel.ConnectorProfile;
import de.fhg.aisec.ids.api.infomodel.InfoModel;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;
import de.fraunhofer.iais.eis.AppExecutionResources;
import de.fraunhofer.iais.eis.AuditLogging;
import de.fraunhofer.iais.eis.AuthenticationSupport;
import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.ConnectorBuilder;
import de.fraunhofer.iais.eis.DataUsageControlSupport;
import de.fraunhofer.iais.eis.IntegrityProtectionAndVerification;
import de.fraunhofer.iais.eis.IntegrityProtectionScope;
import de.fraunhofer.iais.eis.SecurityProfile;
import de.fraunhofer.iais.eis.SecurityProfileBuilder;
import de.fraunhofer.iais.eis.ServiceIsolationSupport;
import de.fraunhofer.iais.eis.idsLocalDataConfidentiality;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;
import de.fraunhofer.iais.eis.util.PlainLiteral;


/**
 * REST API interface for settings in the connector.
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/settings/<method>.
 */
@Path("/settings")
public class SettingsApi {
	private static final Logger LOG = LoggerFactory.getLogger(SettingsApi.class);
	private static final String CONNECTOR_MODEL = "ids.model";
	
    @POST
    @Path("/connectorProfile")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postConnectorProfile(ConnectorProfile cP) {
    	URL conn_url = null;
    	URL op_url = null;
    	SecurityProfile prof = null;
    	PlainLiteral entityName = null;
    	Connector c = null;
    	InfoModel im = null;
    	
    	try {
    		conn_url = new URL(cP.getConnectorURL());
    	} catch (MalformedURLException ex) {
    		LOG.error("Caught ConstraintViolationException while building Security profile.", ex);
			return Response.status(500).entity("Caught MalformedURLException while building Connector URL.").build();
    	}
    	
    	try {
    		op_url = new URL(cP.getOperatorURL());
    	} catch (MalformedURLException ex) {
    		LOG.error("Caught ConstraintViolationException while building Security profile.", ex);
			return Response.status(500).entity("Caught MalformedURLException while building Operator URL.").build();
    	}
    	
    	entityName = new PlainLiteral(cP.getConnectorEntityName());
    	
    	try {
    			prof = new SecurityProfileBuilder()
				.integrityProtectionAndVerification(IntegrityProtectionAndVerification
						.valueOf(cP.getIntegrityProtectionAndVerification()))
				.authenticationSupport(AuthenticationSupport.valueOf(cP.getAuthenticationSupport()))
				.serviceIsolationSupport(ServiceIsolationSupport.valueOf(cP.getServiceIsolation()))
				.integrityProtectionScope(IntegrityProtectionScope.valueOf(cP.getIntegrityProtectionVerificationScope()))
				.appExecutionResources(AppExecutionResources.valueOf(cP.getAppExecutionResources()))
				.dataUsageControlSupport(DataUsageControlSupport.valueOf(cP.getDataUsageControlSupport()))
				.auditLogging(AuditLogging.valueOf(cP.getAuditLogging()))
				.localDataConfidentiality(idsLocalDataConfidentiality.valueOf(cP.getLocalDataConfidentiality()))
				.build();
    	} catch (ConstraintViolationException ex) {
			LOG.error("Caught ConstraintViolationException while building Security profile.", ex);
			return Response.status(500).build();
		}
    	
    	//Store Connector object to Preferences
    	im = WebConsoleComponent.getInfoModelManagerOrThrowSUE();
    	if(im.setConnector(conn_url, op_url, Arrays.asList(entityName), prof))
    		return Response.status(200).entity("Connector object successfully stored.").build();
    	else
    		return Response.status(500).entity("Connector object couldn't be stored.").build();
    }
    
    @GET
    @Path("/connectorProfile")
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectorProfile getConnectorProfile() {
       
    	return new ConnectorProfile();
    }

    /*
	@GET
	@Path("/testInfoModel")
	@Produces(MediaType.APPLICATION_JSON)
	public String testInfoModel(){
		WebConsoleComponent.getInfoModelManagerOrThrowSUE();
		return "Infomodel not null.";
	}*/

	/*
    @POST
    @Path("/postJSON")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postStrMsg( String msg, URL url) {
        String output = "POST:Jersey say : " + msg;
        return Response.status(200).entity(output).build();
    }*/
    
}
