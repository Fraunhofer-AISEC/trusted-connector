package de.fhg.aisec.ids.webconsole.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
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
		List<String> result = new ArrayList<>();
		
		Optional<PAP> pap = WebConsoleComponent.getPolicyAdministrationPoint();
		if (pap.isPresent()) {
			result = pap.get().listRules();
		}
		
		return new GsonBuilder().create().toJson(result);
	}
}