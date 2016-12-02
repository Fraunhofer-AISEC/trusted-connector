package de.fhg.ids.attestation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.fhg.ids.comm.ws.protocol.rat.*;

import com.google.gson.Gson;

@Path("/")
public class REST {
	
	private PcrMessage message;
	private String index;
	private Database db;
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String index() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return new BufferedReader(new InputStreamReader(loader.getResourceAsStream("index.html"))).lines().collect(Collectors.joining("\n"));
	}
	
	@GET
	@Path("administration")
	@Produces(MediaType.TEXT_HTML)
	public String administration() {
		return "<html><body>remote attestation repository admin interface</body></html>";
	}
	
	@POST
	@Path("post")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public String postMethod(@FormParam("data") String jsonData) {
		Gson gson = new Gson();
		message = gson.fromJson(jsonData, PcrMessage.class);
		if(this.checkPcrValues(message)) {
			message.setSuccess(true);
		}
		else {
			message.setSuccess(false);
		}
		this.signMessage(message);
		return gson.toJson(message);
	}

	private boolean checkPcrValues(PcrMessage message) {
		// TODO Auto-generated method stub
		return false;
	}

	private void signMessage(PcrMessage message) {
		// TODO Auto-generated method stub
		
	}
}
