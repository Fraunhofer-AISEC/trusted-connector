package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fraunhofer.aisec.tpm2j.tools.ByteArrayUtil;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryResponse;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.unixsocket.UnixSocketResponsHandler;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;


public class RemoteAttestationConsumerHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationConsumerHandler.class);
	private UnixSocketResponsHandler handler;
	private UnixSocketThread client;
	private Thread thread;
	private long sessionID = 0;		// used to count messages between ids connectors during attestation
	private URI ttpUri;
	private boolean repoCheck = false;
	private boolean success = false;
	
	public RemoteAttestationConsumerHandler(FSM fsm, IdsAttestationType type, URI ttpUri, String socket) {
		// set ttp uri
		this.ttpUri = ttpUri;
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type (see attestation.proto)
		this.aType = type;
		// try to start new Thread:
		// UnixSocketThread will be used to communicate with local TPM2d
		try {
			// client will be used to send messages
			this.client = new UnixSocketThread(socket);
			this.thread = new Thread(client);
			this.thread.setDaemon(true);
			this.thread.start();
			// responseHandler will be used to wait for messages
			this.handler = new UnixSocketResponsHandler();
		} catch (IOException e) {
			lastError = "could not write to/read from " + socket;
			LOG.debug(lastError);
			e.printStackTrace();
		}
	}
	
	public boolean isSuccessful() {
		return this.success;
	}

	public MessageLite enterRatRequest(Event e) {
		// generate a new software nonce on the client and send it to server
		this.myNonce = NonceGenerator.generate();
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
			if(thread.isAlive()) {
				try {
					// send msg to local unix socket
					// construct protobuf message to send to local tpm2d via unix socket
					ControllerToTpm msg = ControllerToTpm
							.newBuilder()
							.setAtype(this.aType)
							.setQualifyingData(this.yourNonce)
							.setCode(Code.INTERNAL_ATTESTATION_REQ)
							.build();
					client.send(msg.toByteArray(), this.handler);
					// and wait for response
					byte[] toParse = this.handler.waitForResponse();
					TpmToController response = TpmToController.parseFrom(toParse);
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
				} catch (InterruptedException ex) {
					lastError = "error: InterruptedException when talking to tpm2d :" + ex.getMessage();
				}
			}
			else {
				lastError = "error: RAT client thread is not alive !";
			}	
		}
		else {
			lastError = "error: repository entries do not match";
		}
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
	}

	public MessageLite sendResult(Event e) {
		if(this.checkSignature(e.getMessage().getAttestationResponse(), this.myNonce)) {
			if(++this.sessionID == e.getMessage().getId()) {
				if(RemoteAttestationHandler.checkRepository(this.aType, NonceGenerator.generate(), e.getMessage().getAttestationResponse(), ttpUri)) {
					this.success = true;
				}
				else {
					this.success = false;
				}
				return ConnectorMessage
						.newBuilder()
						.setId(++this.sessionID)
						.setType(ConnectorMessage.Type.RAT_RESULT)
						.setAttestationResult(
								AttestationResult
								.newBuilder()
								.setAtype(this.aType)
								.setResult(this.success)
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
}
