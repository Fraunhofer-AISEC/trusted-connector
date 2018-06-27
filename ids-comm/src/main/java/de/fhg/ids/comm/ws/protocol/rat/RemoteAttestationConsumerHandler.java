/*-
 * ========================LICENSE_START=================================
 * Camel IDS Component
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
package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;

public class RemoteAttestationConsumerHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
	private UnixSocketResponseHandler handler;
	private UnixSocketThread tpmClient;
	private Thread thread;
	private long sessionID = 0;		// used to count messages between ids connectors during attestation
	private URI ttpUri;
	private boolean repoCheck = false;
	private boolean yourSuccess = false;
	private boolean mySuccess = false;
	private int attestationMask = 0;
	
	public RemoteAttestationConsumerHandler(FSM fsm, IdsAttestationType type, int attestationMask, URI ttpUri, String socket) {
		// set ttp uri
		this.ttpUri = ttpUri;
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type and mask (see attestation.proto)
		this.aType = type;
		this.attestationMask = attestationMask;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d
		try {
			// client will be used to send messages
			this.tpmClient = new UnixSocketThread(socket);
			this.thread = new Thread(tpmClient);
			this.thread.setDaemon(true);
			this.thread.start();
			// responseHandler will be used to wait for messages
			this.handler = new UnixSocketResponseHandler();
		} catch (IOException e) {
			lastError = "could not write to/read from " + socket;
			LOG.warn(lastError);
		}
	}
	
	public boolean isSuccessful() {
		LOG.debug("your success: " + this.yourSuccess);
		LOG.debug("my success: " + this.mySuccess);		
		return this.yourSuccess && this.mySuccess;
	}

	public MessageLite enterRatRequest(Event e) {
		// generate a new software nonce on the client and send it to server
		this.myNonce = NonceGenerator.generate(40);
		// get starting session id
		this.sessionID = e.getMessage().getId();
		return ConnectorMessage
				.newBuilder()
				.setId(++this.sessionID)
				.setType(ConnectorMessage.Type.RAT_REQUEST)
				.setAttestationRequest(
						AttestationRequest
						.newBuilder()
						.setAtype(this.aType)
						.setQualifyingData(this.myNonce)
						.build())
				.build();			
	}

	public MessageLite sendTPM2Ddata(Event e) {
		// get nonce from server msg
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData().toString();
		if(++this.sessionID == e.getMessage().getId()) {
			if(thread!=null && thread.isAlive()) {
				try {
					ControllerToTpm msg;
					if(this.aType.equals(IdsAttestationType.ADVANCED)) {
						// send msg to local unix socket with bitmask set
						// construct protobuf message to send to local tpm2d via unix socket
						msg = ControllerToTpm
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.yourNonce)
								.setCode(Code.INTERNAL_ATTESTATION_REQ)
								.setPcrs(this.attestationMask)
								.build();
					}
					else {
						// send msg to local unix socket
						// construct protobuf message to send to local tpm2d via unix socket
						msg = ControllerToTpm
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.yourNonce)
								.setCode(Code.INTERNAL_ATTESTATION_REQ)
								.build();
					}
					tpmClient.send(msg.toByteArray(), this.handler, true);
					// and wait for response
					byte[] toParse = this.handler.waitForResponse();
					TpmToController response = TpmToController.parseFrom(toParse);
					// now return values from answer to provider
					return ConnectorMessage
							.newBuilder()
							.setId(++this.sessionID)
							.setType(ConnectorMessage.Type.RAT_RESPONSE)
							.setAttestationResponse(
									AttestationResponse
									.newBuilder()
									.setAtype(this.aType)
									.setQualifyingData(this.yourNonce)
									.setHalg(response.getHalg())
									.setQuoted(response.getQuoted())
									.setSignature(response.getSignature())
									.addAllPcrValues(response.getPcrValuesList())
									.setCertificateUri(response.getCertificateUri())
									.build()
									)
							.build();
				} catch (IOException ex) {
					lastError = "error: IOException when talking to tpm2d :" + ex.getMessage();
					tpmClient.terminate();
				} catch (InterruptedException ex) {
					lastError = "error: InterruptedException when talking to tpm2d :" + ex.getMessage();
					tpmClient.terminate();
				}
			} else {
				lastError = "error: RAT client thread is not alive. No TPM present? ";
			}	
		} else {
			lastError = "error: repository entries do not match";
		}
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
	}

	public MessageLite sendResult(Event e) {
		if(this.checkSignature(e.getMessage().getAttestationResponse(), this.myNonce)) {
			if(++this.sessionID == e.getMessage().getId()) {
				if(RemoteAttestationHandler.checkRepository(this.aType, e.getMessage().getAttestationResponse(), ttpUri)) {
					this.mySuccess = true;
				}
				return ConnectorMessage
						.newBuilder()
						.setId(++this.sessionID)
						.setType(ConnectorMessage.Type.RAT_RESULT)
						.setAttestationResult(
								AttestationResult
								.newBuilder()
								.setAtype(this.aType)
								.setResult(this.mySuccess)
								.build())
						.build();
			}
			else {
				lastError = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
			}
		}
		else {
			lastError = "error: signature check not ok";
		}
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);	
	}

	public MessageLite leaveRatRequest(Event e) {
		this.yourSuccess = e.getMessage().getAttestationResult().getResult();
		this.thread.interrupt();
		if(++this.sessionID == e.getMessage().getId()) {
			return ConnectorMessage
					.newBuilder()
					.setId(++this.sessionID)
					.setType(ConnectorMessage.Type.RAT_LEAVE)
					.setAttestationLeave(
							AttestationLeave
							.newBuilder()
							.setAtype(this.aType)
							.build()
							)
					.build();			
		}
		lastError = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
	}
	
	public MessageLite sendNoAttestation(Event e) {
		LOG.debug("we are skipping remote attestation");
		return ConnectorMessage
	    		.newBuilder()
	    		.setType(ConnectorMessage.Type.RAT_REQUEST)
	    		.setId(e.getMessage().getId() + 1)
	    		.build();			
	}	
}
