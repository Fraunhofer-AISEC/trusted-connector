package de.fhg.ids.comm.ws.protocol.rat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

import org.apache.camel.util.jsse.SSLContextParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm;
import de.fhg.aisec.ids.messages.AttestationProtos.ControllerToTpm.Code;
import de.fhg.aisec.ids.messages.AttestationProtos.IdsAttestationType;
import de.fhg.aisec.ids.messages.AttestationProtos.TpmToController;
import de.fhg.aisec.ids.messages.AttestationProtos.Pcr;
import de.fhg.aisec.ids.messages.Idscp.AttestationLeave;
import de.fhg.aisec.ids.messages.Idscp.AttestationResponse;
import de.fhg.aisec.ids.messages.Idscp.AttestationRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationResult;
import de.fhg.aisec.ids.messages.Idscp.ConnectorMessage;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryRequest;
import de.fhg.aisec.ids.messages.Idscp.AttestationRepositoryResponse;
import de.fhg.ids.comm.unixsocket.UnixSocketResponseHandler;
import de.fhg.ids.comm.unixsocket.UnixSocketThread;
import de.fhg.ids.comm.ws.protocol.fsm.Event;
import de.fhg.ids.comm.ws.protocol.fsm.FSM;
import de.fraunhofer.aisec.tpm2j.tpm2b.TPM2B_PUBLIC;
import de.fraunhofer.aisec.tpm2j.tpms.TPMS_ATTEST;
import de.fraunhofer.aisec.tpm2j.tpmt.TPMT_SIGNATURE;

public class RemoteAttestationProviderHandler extends RemoteAttestationHandler {
	private final FSM fsm;
	private String myNonce;
	private String yourNonce;
	private IdsAttestationType aType;
	private Thread thread;
	private Logger LOG = LoggerFactory.getLogger(RemoteAttestationProviderHandler.class);
	private UnixSocketThread client;
	private UnixSocketResponseHandler handler;
	private ConnectorMessage msg;
	private long sessionID = 0;		// used to count messages between ids connectors during attestation
	private URI ttpUri;
	private boolean yourSuccess = false;
	private boolean mySuccess = false;
	private int attestationMask = 0;
	private SSLContextParameters sslParams;
	private AttestationResponse resp;
	
	public RemoteAttestationProviderHandler(FSM fsm, IdsAttestationType type, int attestationMask, URI ttpUri, String socket, SSLContextParameters params) {
		// set ttp uri
		this.ttpUri = ttpUri;
		// set finite state machine
		this.fsm = fsm;
		// set current attestation type and mask (see attestation.proto)
		this.aType = type;
		this.attestationMask = attestationMask;
		this.sslParams = params;
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
			LOG.debug("could not write to/read from " + socket);
			e.printStackTrace();
		}		
	}
	
	public boolean isSuccessful() {
		LOG.debug("your success: " + this.yourSuccess);
		LOG.debug("my success: " + this.mySuccess);
		return this.yourSuccess && this.mySuccess;
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
		// temporarly safe attestation response in order to check it in the result phase
		this.resp = e.getMessage().getAttestationResponse();
		// get nonce from server msg
		if(thread.isAlive()) {
			if(++this.sessionID == e.getMessage().getId()) {
				try {
					ControllerToTpm msg;
					if(this.aType.equals(IdsAttestationType.ADVANCED)) {
						// send msg to local unix socket
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
					LOG.debug(msg.toString());
					client.send(msg.toByteArray(), this.handler, true);
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
				lastError = "error: sessionID not correct ! (is " + e.getMessage().getId()+" but should have been "+ (this.sessionID+1) +")";
			}
		}
		else {
			lastError = "error: RAT client thread is not alive !";
		}
		LOG.debug(lastError);
		return RemoteAttestationHandler.sendError(this.thread, ++this.sessionID, RemoteAttestationHandler.lastError);
	}

	public MessageLite sendResult(Event e) {
		if(this.checkSignature(this.resp, this.myNonce)) {
			if(++this.sessionID == e.getMessage().getId()) {
				if(RemoteAttestationHandler.checkRepository(this.aType, this.resp, ttpUri, this.sslParams)) {
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
	    		.setType(ConnectorMessage.Type.RAT_RESULT)
	    		.setId(e.getMessage().getId() + 1)
	    		.build();			
	}	
}
