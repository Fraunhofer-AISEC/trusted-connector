package de.fhg.aisec.ids.webconsole.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * REST API interface for managing "certification" in the connector. 
 * 
 * The API will be available at http://localhost:8181/cxf/api/v1/certs/<method>.
 * 
 * @author Hamed Rasifard (hamed.rasifard@aisec.fraunhofer.de)
 *
 */

@Path("/certs")
public class CertApi {
	private static final Logger LOG = LoggerFactory.getLogger(AppApi.class);
	
	@GET
	@Path("list")
	@Produces("application/json")
	public String list() {
		List<mockedCert> certs = new ArrayList<>();
		
	     mockedCert cert1 = new mockedCert();
	     mockedCert cert2 = new mockedCert();
	     
	     cert1.id = "cert1";
	     cert1.name = "This is certification one";
	     cert1.publicKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.  Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
	     
	     cert2.id = "cert2";
	     cert2.name = "This is certification two";
	     cert2.publicKey = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.  Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.Mauris sagittis pellentesque lacus eleifend lacinia...Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
	     
	     certs.add(cert1);
	     certs.add(cert2);
		
	    return new GsonBuilder().create().toJson(certs);
	}
	
	@GET
	@Path("delete")
	@Produces("application/json")
	public String delete(@QueryParam("ceritficateId") String certificateId) {
		
		return new Gson().toJson(true);
	}
	
	public class mockedCert {
		public String name;
		public String id;
		public String publicKey;

	}
}


