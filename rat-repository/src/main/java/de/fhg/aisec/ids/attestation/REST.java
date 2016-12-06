package de.fhg.aisec.ids.attestation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.fhg.ids.comm.ws.protocol.rat.*;

import com.google.gson.Gson;

@Path("/")
public class REST {
	
	private PcrMessage message;
	private String index;
	private Database db;
	
	public REST() {
		try {
			this.db = new Database();
		} catch (SQLException e) {
			System.out.println("could not connect to SQLite db.");
			e.printStackTrace();
		}
	}
	
	@GET
	@Path("/configurations/list")
	@Produces(MediaType.TEXT_HTML)
	public String configurationList() {
		Gson gson = new Gson();
		try {
			return gson.toJson(this.db.getConfigurations());
		} catch (SQLException e) {
			System.out.println("could not gsonify configurations list.");
			e.printStackTrace();
			return "";
		}
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
	
	// SIMPLE FILE TRANSFER FROM HERE ON

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String index() {
		return this.getFile("webapp/index.html");
	}

	@GET
	@Path("admin")
	@Produces(MediaType.TEXT_HTML)
	public String admin() {
		return this.getFile("webapp/admin.html");
	}	

	@GET
	@Path("/html/{filename}")
	@Produces(MediaType.TEXT_HTML)
	public String htmlFile(@PathParam("filename") String filename) {
		return this.getFile("webapp/html/" + filename);
	}
	
	@GET
	@Path("/js/{filename}")
	@Produces(MediaType.TEXT_HTML)
	public String jsFile(@PathParam("filename") String filename) {
		return this.getFile("webapp/js/" + filename);
	}
	
	private String getFile(String file) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return new BufferedReader(new InputStreamReader(loader.getResourceAsStream(file))).lines().collect(Collectors.joining("\n"));
	}
}
