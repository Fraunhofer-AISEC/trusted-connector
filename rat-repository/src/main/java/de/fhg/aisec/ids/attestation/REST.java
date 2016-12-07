package de.fhg.aisec.ids.attestation;

import java.io.BufferedReader;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.rat.*;

import com.google.gson.Gson;

@Path("/")
public class REST {
	
	private PcrMessage message;
	private Logger LOG = LoggerFactory.getLogger(REST.class);
	private Database db;
	private String ret;
	
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
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfigurationList() {
		try {
			ret = this.db.getConfigurationList();
		} catch (Exception e) {
			ret = e.getMessage();
		}
		return ret;
	}
	
	@GET
	@Path("/configurations/{cid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfiguration(@PathParam("cid") String cid) {
		if(isInteger(cid)) {
			long id = Long.parseLong(cid);
			ret = this.db.getConfiguration(id);
		}
		else {
			ret = "id " + cid + " is not an Integer!";				
		}
		return ret;
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
	
	private static boolean isInteger(String s) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i), 10) < 0) return false;
	    }
	    return true;
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
	@Produces(MediaType.TEXT_PLAIN)
	public String jsFile(@PathParam("filename") String filename) {
		return this.getFile("webapp/js/" + filename);
	}
	
	private String getFile(String file) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return new BufferedReader(new InputStreamReader(loader.getResourceAsStream(file))).lines().collect(Collectors.joining("\n"));
	}
}
