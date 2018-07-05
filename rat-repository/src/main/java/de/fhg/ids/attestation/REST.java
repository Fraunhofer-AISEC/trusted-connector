/*-
 * ========================LICENSE_START=================================
 * rat-repository
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
package de.fhg.ids.attestation;

import com.google.gson.Gson;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.Error;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;

@Path("/")
public class REST {

	private Database db;
	private Gson gson = new Gson();
	private static final String PROTOBUF_URL = "/configurations/check";
	private boolean corsEnabled = true;
	protected @Context HttpServletResponse response;
	
	public REST(Database db) {
		this.db = db;
	}
	
	@POST
	@Path(PROTOBUF_URL)
	@Produces(MediaTypeExt.APPLICATION_PROTOBUF)
	@Consumes(MediaTypeExt.APPLICATION_PROTOBUF)
	public ConnectorMessage checkConfiguration(ConnectorMessage msg) {
		if(msg.getType().equals(ConnectorMessage.Type.RAT_REPO_REQUEST)) {
			try {
				AttestationRepositoryRequest request = msg.getAttestationRepositoryRequest();
				int numPcrValues = request.getPcrValuesCount();
				IdsAttestationType type = request.getAtype();
				// check if attestation type matches # opf pcr values
				if(numPcrValues > 0) {
					switch(type) {
						case BASIC:
							if(numPcrValues == 11) {
								return this.checkMessage(msg);
							}
							else {
								return this.sendError("error: IdsAttestationType is BASIC, but number of PCR values (\""+numPcrValues+"\") send is not 11", msg.getId());
							}
						case ADVANCED:
							return this.checkMessage(msg);					
						case ALL:
							if(numPcrValues == 24) {
								return this.checkMessage(msg);
							}
							else {
								return this.sendError("error: IdsAttestationType is ALL, but number of PCR values (\""+numPcrValues+"\") send is not 24", msg.getId());
							}
						default:
							return this.sendError("error: unknown IdsAttestationType", msg.getId());
					}
				}
				else {
					return this.sendError("there were no PCR values in the request", msg.getId());
				}
			}
			catch (Exception e) {
				return this.sendError(e.getMessage(), msg.getId());
			}
		}
		else {
			return this.sendError("request was not of type RAT_REPO_REQUEST", msg.getId());
		}
	}
	
	// get all configurations using json
	@GET
	@Path("/json/configurations/")
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfigurationList() {
		this.setCORSHeader(response, corsEnabled);
		String ret;
		try {
			ret = gson.toJson(this.db.getConfigurationList());
		} catch (Exception e) {
			ret = e.getMessage();
		}
		return ret;
	}

	// get a single configuration with id {cid} using json	
	@GET
	@Path("/json/configurations/{cid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getConfiguration(@PathParam("cid") String cid) {
		this.setCORSHeader(response, corsEnabled);
		if(isInteger(cid)) {
			return gson.toJson(this.db.getConfiguration(Long.parseLong(cid)));
		} else {
			throw new BadRequestException("id " + cid + " is not an Integer!");
		}
	}

	// post a new configuration	
	@POST
	@Path("/json/configurations/new")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public long addConfiguration(Configuration config) {
		this.setCORSHeader(response, corsEnabled);
		try {
			Long[] existing = this.db.getConfigurationId(config.getValues());
			if(existing.length == 0) {
				return this.db.insertConfiguration(config.getName(), config.getType(), config.getValues());
			} else {
				throw new InternalServerErrorException();
			}
		} catch (Exception e) {
			throw new InternalServerErrorException(e);
		}
	}
	
	// delete a single configuration with id {cid} using json	
	@DELETE
	@Path("/json/configurations/{cid}")
	@Produces(MediaType.APPLICATION_JSON)
	public String deleteConfiguration(@PathParam("cid") String cid) throws SQLException {
		this.setCORSHeader(response, corsEnabled);
		if(isInteger(cid)) {
			if(this.db.deleteConfigurationById(Integer.parseInt(cid))) {
				return cid;
			} else {
				throw new InternalServerErrorException();
			}
		} else {
			throw new InternalServerErrorException();
		}
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
	
	private void setCORSHeader(HttpServletResponse response, boolean enable) {
		response.setCharacterEncoding("utf-8");
		if(enable) {
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
			response.setHeader("Access-Control-Allow-Credentials", "true");
			response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
			response.setHeader("Access-Control-Max-Age", "1209600");			
		}
	}
	
	private ConnectorMessage checkMessage(ConnectorMessage msg) {
		return ConnectorMessage
				.newBuilder()
				.setId(msg.getId() + 1)
				.setType(ConnectorMessage.Type.RAT_REPO_RESPONSE)
				.setAttestationRepositoryResponse(
						AttestationRepositoryResponse
		        		.newBuilder()
		        		.setAtype(msg.getAttestationRepositoryRequest().getAtype())
		        		.setResult(this.db.checkMessage(msg))
		        		.setQualifyingData(msg.getAttestationRepositoryRequest().getQualifyingData())
		        		.setSignature("")
		        		.setCertificateUri("")
		        		.build()
						)
				.build();
	}
	
	private ConnectorMessage sendError(String error, long id) {
		return ConnectorMessage
				.newBuilder()
				.setId(id + 1)
				.setType(ConnectorMessage.Type.ERROR)
				.setError(
						Error
						.newBuilder()
						.setErrorCode("")
						.setErrorMessage(error)
						.build()
						)
				.build();
	}
}
