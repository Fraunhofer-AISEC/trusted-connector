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
import java.util.List;

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

/**
 * Implements the handling of individual protocol steps in the IDS remote
 * attestation protocol.
 *
 */
public class RemoteAttestationProviderHandler extends RemoteAttestationHandler {
	private static final Logger LOG = LoggerFactory.getLogger(RemoteAttestationProviderHandler.class);
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Thread thread;
	private UnixSocketThread client;
	private UnixSocketResponseHandler handler;
	private ConnectorMessage msg;
	private long sessionID = 0;		// used to count messages between ids connectors during attestation
	private URI ttpUri;
	private int attestationMask = 0;
	private AttestationResponse resp;
	
	public RemoteAttestationProviderHandler(FSM fsm, IdsAttestationType type, int attestationMask, URI ttpUri, String socket) {
		// set ttp uri
		this.ttpUri = ttpUri;
		// set current attestation type and mask (see attestation.proto)
		this.aType = type;
		this.attestationMask = attestationMask;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d		
		try {
			// client will be used to send messages
			this.client = new UnixSocketThread(socket);
			this.thread = new Thread(client);
			this.thread.setDaemon(true);
			this.thread.start();
			// responseHandler will be used to wait for messages
			this.handler = new UnixSocketResponseHandler();
		} catch (IOException e) {
			LOG.warn("could not write to/read from " + socket);
			if (client != null) {
				this.client.terminate();
			}
		}		
	}
	
	public MessageLite enterRatRequest(Event e) {
		this.yourNonce = e.getMessage().getAttestationRequest().getQualifyingData();
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
		// temporarily save attestation response in order to check it in the result phase
		this.resp = e.getMessage().getAttestationResponse();

		if(++this.sessionID != e.getMessage().getId()) {
			return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")");
		}
		
		String halg = "";
		String quoted = "";
		String signature = "";
		List<Pcr> pcrValues = new ArrayList<>();
		String certificateUri = "";
		if(thread!=null && thread.isAlive()) {
				try {
					ControllerToTpm msg;
					if (this.aType.equals(IdsAttestationType.ADVANCED)) {
						// send msg to local unix socket
						// construct protobuf message to send to local tpm2d via unix socket
						msg = ControllerToTpm
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.yourNonce)
								.setCode(Code.INTERNAL_ATTESTATION_REQ)
								.setPcrs(this.attestationMask)
								.build();	
					} else {
						// send msg to local unix socket
						// construct protobuf message to send to local tpm2d via unix socket
						msg = ControllerToTpm
								.newBuilder()
								.setAtype(this.aType)
								.setQualifyingData(this.yourNonce)
								.setCode(Code.INTERNAL_ATTESTATION_REQ)
								.build();	
					}
					LOG.debug(msg.toString());
					client.send(msg.toByteArray(), this.handler, true);
					// and wait for response
					byte[] toParse = this.handler.waitForResponse();
					TpmToController response = TpmToController.parseFrom(toParse);
					halg = response.getHalg();
					quoted = response.getQuoted();
					signature = response.getSignature();
					pcrValues = response.getPcrValuesList();
					certificateUri = response.getCertificateUri();
				} catch (IOException ex) {
					lastError = "error: IOException when talking to tpm2d :" + ex.getMessage();
					client.terminate();
				} catch (InterruptedException ex) {
					lastError = "error: InterruptedException when talking to tpm2d :" + ex.getMessage();
					client.terminate();
					Thread.currentThread().interrupt();
				}
		} else {
			lastError = "error: RAT client thread is not alive !";
		}
		// now return values from answer to server
		return ConnectorMessage
				.newBuilder()
				.setId(++this.sessionID)
				.setType(ConnectorMessage.Type.RAT_RESPONSE)
				.setAttestationResponse(
						AttestationResponse
						.newBuilder()
						.setAtype(this.aType)
						.setQualifyingData(this.yourNonce)
						.setHalg(halg)
						.setQuoted(quoted)
						.setSignature(signature)
						.addAllPcrValues(pcrValues)
						.setCertificateUri(certificateUri)
						.build()
						)
				.build();
	}

	public MessageLite sendResult(Event e) {
		if(++this.sessionID == e.getMessage().getId()) {
			if(this.checkSignature(this.resp, this.myNonce)) {
				if(RemoteAttestationHandler.checkRepository(this.aType, this.resp, ttpUri)) {
					this.mySuccess = true;					
				}
			} else {
				lastError = "error: signature check not ok";
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
		} else {
			lastError = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
		}
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);

	}

	public MessageLite leaveRatRequest(Event e) {
		this.yourSuccess = e.getMessage().getAttestationResult().getResult();
		if (this.thread != null) {
			this.thread.interrupt();
		}
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
}
