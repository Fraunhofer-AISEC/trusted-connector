package de.fhg.ids.attestation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;

@Path("/")
public class REST {
	
	private ConnectorMessage message;
	private Logger LOG = LoggerFactory.getLogger(REST.class);
	private Database db;
	private String ret;
	protected @Context HttpServletResponse response;
	
	public REST(Database db) {
		this.db = db;
	}
	
	public REST() {
	}
	
	@POST
	@Path("/configurations/check")
	@Produces(MediaTypeExt.APPLICATION_PROTOBUF)
	@Consumes(MediaTypeExt.APPLICATION_PROTOBUF)
	public ConnectorMessage checkConfiguration(ConnectorMessage msg) throws SQLException {
		if(msg.getType().equals(ConnectorMessage.Type.RAT_REPO_REQUEST)) {
			if(msg.getAttestationRepositoryRequest().getPcrValuesCount() > 0) {
				LOG.debug("REPO msg to check : " + msg.toString());
				int numPcrValues = msg.getAttestationResponse().getPcrValuesCount();
				if(this.db.checkMessage(msg)) {
					return ConnectorMessage
							.newBuilder()
							.setId(msg.getId() + 1)
							.setType(ConnectorMessage.Type.RAT_REPO_RESPONSE)
							.setAttestationRepositoryResponse(
									AttestationRepositoryResponse
					        		.newBuilder()
					        		.setAtype(IdsAttestationType.BASIC)
					        		.setResult(true)
					        		.setQualifyingData(msg.getAttestationRepositoryRequest().getQualifyingData())
					        		.setSignature("")
					        		.setCertificateUri("")
					        		.build()
									)
							.build();
				}
				else {
					return ConnectorMessage
							.newBuilder()
							.setId(msg.getId() + 1)
							.setType(ConnectorMessage.Type.RAT_REPO_RESPONSE)
							.setAttestationRepositoryResponse(
									AttestationRepositoryResponse
					        		.newBuilder()
					        		.setAtype(IdsAttestationType.BASIC)
					        		.setResult(false)
					        		.setQualifyingData(msg.getAttestationRepositoryRequest().getQualifyingData())
					        		.setSignature("")
					        		.setCertificateUri("")
					        		.build()
									)
							.build();
				}
			}
			else {
				return ConnectorMessage
						.newBuilder()
						.setId(msg.getId() + 1)
						.setType(ConnectorMessage.Type.ERROR)
						.setError(
								Error
								.newBuilder()
								.setErrorCode("")
								.setErrorMessage("there were no PCR values in the request")
								.build()
								)
						.build();
			}
		}
		else {
			return ConnectorMessage
					.newBuilder()
					.setId(msg.getId() + 1)
					.setType(ConnectorMessage.Type.ERROR)
					.setError(
							Error
							.newBuilder()
							.setErrorCode("")
							.setErrorMessage("request was not of type RAT_REPO_REQUEST")
							.build()
							)
					.build();
		}
	}
	
	@GET
	@Path("/configurations/list")
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfigurationList() {
		response.setCharacterEncoding("utf-8");
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
		response.setCharacterEncoding("utf-8");
		Gson gson = new Gson();
		if(isInteger(cid)) {
			ret = gson.toJson(this.db.getConfiguration(Long.parseLong(cid)));
		}
		else {
			ret = "id " + cid + " is not an Integer!";				
		}
		return ret;
	}
	
	@GET
	@Path("/configurations/delete/{cid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteConfiguration(@PathParam("cid") String cid) throws NumberFormatException, SQLException {
		response.setCharacterEncoding("utf-8");
		Gson gson = new Gson();
		if(isInteger(cid)) {
			if(this.db.deleteConfigurationById(Integer.parseInt(cid))) {
				return gson.toJson(true);
			}
			else {
				return gson.toJson(false);
			}
		}
		else {
			ret = "id " + cid + " is not an Integer!";				
		}
		return ret;
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
		response.setCharacterEncoding("utf-8");
		return this.getFile("webapp/index.html");
	}

	@GET
	@Path("admin")
	@Produces(MediaType.TEXT_HTML)
	public String admin() {
		response.setCharacterEncoding("utf-8");
		return this.getFile("webapp/admin.html");
	}	

	@GET
	@Path("/html/{filename}")
	@Produces(MediaType.TEXT_HTML)
	public String htmlFile(@PathParam("filename") String filename) {
		response.setCharacterEncoding("utf-8");
		return this.getFile("webapp/html/" + filename);
	}
	
	@GET
	@Path("/js/{filename}")
	@Produces(MediaType.TEXT_PLAIN)
	public String jsFile(@PathParam("filename") String filename) {
		response.setCharacterEncoding("utf-8");
		return this.getFile("webapp/js/" + filename);
	}
	
	private String getFile(String file) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return new BufferedReader(new InputStreamReader(loader.getResourceAsStream(file))).lines().collect(Collectors.joining("\n"));
	}
}
