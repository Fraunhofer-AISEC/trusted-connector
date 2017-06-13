package de.fhg.aisec.ids.webconsole.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.policy.PAP;
import de.fhg.aisec.ids.webconsole.WebConsoleComponent;

/**
 * REST API interface for managing "apps" in the connector.
 * 
 * In this implementation, apps are either docker or trustX containers. 
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/policies/<method>.
 * 
 * @author Julian Schuette (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Path("/policies")
public class PolicyApi {
	private static final Logger LOG = LoggerFactory.getLogger(PolicyApi.class);
	
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {
		LOG.info("policy list");
		List<String> result = new ArrayList<>();
		
		Optional<PAP> pap = WebConsoleComponent.getPolicyAdministrationPoint();
		if (!pap.isPresent()) {
			return "[]";
		}		
		result = pap.get().listRules();
		return new GsonBuilder().create().toJson(result);
	}
	
	@POST
	@OPTIONS
	@Path("install")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String install(@Context HttpServletRequest request, @FormParam("policy_file") InputStream fileInputStream) {
		LOG.info("Received policy file");
		Optional<PAP> cO = WebConsoleComponent.getPolicyAdministrationPoint();
		
		// if pap service is not available at runtime, return empty map
		if (!cO.isPresent()) {
			return "no PAP";
		}
		cO.get().loadPolicy(fileInputStream);
		return new GsonBuilder().create().toJson("OK");
	}	
}